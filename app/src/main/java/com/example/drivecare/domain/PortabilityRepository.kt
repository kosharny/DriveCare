package com.example.drivecare.domain

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import com.example.drivecare.R
import com.example.drivecare.core.model.*
import com.example.drivecare.data.billing.BillingRepository
import com.example.drivecare.data.local.AttachmentStore
import com.example.drivecare.data.local.DriveCareDatabase
import com.example.drivecare.data.local.PreferencesRepository
import com.example.drivecare.data.repository.DriveCareRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Currency
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PortableBackup(
    val version: Int,
    val reportingZone: String,
    val vehicles: List<Vehicle>,
    val records: List<JournalRecord>,
    val plans: List<MaintenancePlan>,
    val conversations: List<Conversation>,
    val attachments: List<Attachment>
)

@Singleton
class PortabilityRepository
@Inject
constructor(
    @ApplicationContext private val applicationContext: Context,
    private val repository: DriveCareRepository,
    private val preferences: PreferencesRepository,
    private val database: DriveCareDatabase,
    private val files: AttachmentStore,
    private val billing: BillingRepository
) {

    private val context
        get() = ContextCompat.getContextForLanguage(applicationContext)

    suspend fun backup(uri: Uri) = withContext(Dispatchers.IO) {
        val snapshot =
            database.withTransaction {
                val dao = database.dao()
                StoreSnapshot(
                    dao.allVehicles(),
                    dao.allRecords(),
                    dao.allPlans(),
                    dao.allConversations(),
                    dao.allAttachments(),
                    loaded = true
                )
            }
        val backup =
            PortableBackup(
                version = 2,
                reportingZone = preferences.preferences.first().reportingZone,
                vehicles = snapshot.vehicles,
                records = snapshot.records,
                plans = snapshot.plans,
                conversations = snapshot.conversations,
                attachments = snapshot.attachments
            )
        validate(backup)
        val manifestData = Json.encodeToString(backup).toByteArray(Charsets.UTF_8)
        require(manifestData.size <= 15 * 1024 * 1024)
        val sourceFiles =
            (
                snapshot.attachments.map { it.path } +
                    snapshot.vehicles.mapNotNull { it.photo }
                ).distinct()
        require(sourceFiles.size <= 5000)
        val expandedSize =
            sourceFiles.fold(manifestData.size.toLong()) { total, path ->
                val size = files.file(path).length()
                require(size in 1..15 * 1024 * 1024)
                Math.addExact(total, size)
            }
        require(expandedSize <= 100 * 1024 * 1024)
        context.contentResolver.openOutputStream(uri, "wt")!!.use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifestData)
                zip.closeEntry()
                val references =
                    snapshot.attachments.map {
                        it.path
                    } +
                        snapshot.vehicles.mapNotNull {
                            it.photo
                        }
                references.distinct().forEach { path ->
                    zip.putNextEntry(ZipEntry("attachments/$path"))
                    files.file(path).inputStream().use {
                        it.copyTo(zip)
                    }
                    zip.closeEntry()
                }
            }
        }
    }

    suspend fun restore(uri: Uri) = withContext(Dispatchers.IO) {
        val staging =
            File(context.cacheDir, "restore-${newId()}").apply {
                mkdirs()
            }
        val created = mutableListOf<File>()
        var committed = false
        var replacedFiles = emptySet<String>()
        try {
            var total = 0L
            var entries = 0
            val names = mutableSetOf<String>()
            context.contentResolver.openInputStream(uri)!!.use { source ->
                ZipInputStream(source).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        require(++entries <= 5001)
                        require(names.add(entry.name))
                        require(!entry.isDirectory)
                        require(
                            entry.name == "manifest.json" ||
                                entry.name.matches(
                                    Regex("attachments/[a-zA-Z0-9-]+\\.[a-z0-9]+")
                                )
                        )
                        val destination = File(staging, entry.name)
                        require(
                            destination.canonicalPath.startsWith(
                                staging.canonicalPath + File.separator
                            )
                        )
                        destination.parentFile!!.mkdirs()
                        destination.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var size = 0L
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                size += count
                                total += count
                                require(size <= 15 * 1024 * 1024 && total <= 100 * 1024 * 1024)
                                output.write(buffer, 0, count)
                            }
                        }
                    }
                }
            }
            val manifest = File(staging, "manifest.json")
            require(manifest.length() in 1..15 * 1024 * 1024)
            val backup = Json.decodeFromString<PortableBackup>(manifest.readText())
            validate(backup)
            val refs =
                (
                    backup.attachments.map {
                        it.path
                    } +
                        backup.vehicles.mapNotNull {
                            it.photo
                        }
                    )
                    .toSet()
            require(
                names == setOf("manifest.json") +
                    refs.map {
                        "attachments/$it"
                    }
            )
            val mapping =
                refs.associateWith { old ->
                    "${newId()}.${old.substringAfterLast('.')}"
                }
            mapping.forEach { (old, new) ->
                val destination = files.file(new)
                created += destination
                File(staging, "attachments/$old").copyTo(destination)
            }
            database.withTransaction {
                val dao = database.dao()
                replacedFiles = dao.allAttachments().map { it.path }.toSet() +
                    dao.allVehicles().mapNotNull { it.photo }
                dao.clearVehicles()
                backup.vehicles.forEach {
                    dao.putVehicle(it.copy(photo = it.photo?.let(mapping::getValue)))
                }
                backup.plans.forEach {
                    dao.putPlan(it.copy(lastNotifiedDay = null))
                }
                backup.records.forEach {
                    dao.putRecord(it)
                }
                backup.conversations.forEach {
                    dao.putConversation(it)
                }
                backup.attachments.forEach {
                    dao.putAttachment(it.copy(path = mapping.getValue(it.path)))
                }
                backup.plans.forEach { plan ->
                    dao.putPlan(
                        MaintenanceRules.recalculate(
                            plan,
                            backup.records.filter {
                                it.vehicleId == plan.vehicleId
                            }
                        )
                    )
                }
            }
            committed = true
            replacedFiles.forEach { files.file(it).delete() }
            com.example.drivecare.core.notifications
                .ReminderScheduler(applicationContext)
                .reconcile()
            preferences.setString("zone", backup.reportingZone)
            preferences.setString(
                "vehicle",
                backup.vehicles.firstOrNull {
                    !it.archived
                }?.id ?: ""
            )
        } finally {
            staging.deleteRecursively()
            if (!committed) {
                created.forEach {
                    it.delete()
                }
            }
        }
    }

    private fun validate(backup: PortableBackup) {
        require(backup.version in 1..2)
        ZoneId.of(backup.reportingZone)
        require(
            backup.vehicles.size <= 1000 &&
                backup.records.size <= 100_000 &&
                backup.plans.size <= 10_000 &&
                backup.conversations.size <= 5000 &&
                backup.attachments.size <= 5000
        )
        fun ids(values: List<String>) {
            require(values.toSet().size == values.size)
            values.forEach {
                java.util.UUID.fromString(it)
            }
        }
        ids(
            backup.vehicles.map {
                it.id
            }
        )
        ids(
            backup.records.map {
                it.id
            }
        )
        ids(
            backup.plans.map {
                it.id
            }
        )
        ids(
            backup.conversations.map {
                it.id
            }
        )
        ids(
            backup.attachments.map {
                it.id
            }
        )
        val vehicles =
            backup.vehicles.associateBy {
                it.id
            }
        val records =
            backup.records.associateBy {
                it.id
            }
        val plans =
            backup.plans.associateBy {
                it.id
            }
        fun decimal(value: String?) {
            if (value != null) {
                require(
                    value.length <= 40 &&
                        value.decimal() >= BigDecimal.ZERO &&
                        value.decimal() <= BigDecimal("1000000000")
                )
            }
        }
        fun bounded(value: String, maximum: Int = 10_000) {
            require(value.length <= maximum)
        }
        backup.vehicles.forEach {
            require(
                it.make.isNotBlank() &&
                    it.model.isNotBlank() &&
                    it.year in 1886..LocalDate.now().year + 1
            )
            listOf(
                it.make,
                it.model,
                it.nickname,
                it.generation,
                it.engine,
                it.transmission,
                it.market
            )
                .forEach { value ->
                    bounded(value, 250)
                }
            Currency.getInstance(it.currency)
            it.purchaseDate?.let(LocalDate::parse)
            decimal(it.purchaseOdometerKm)
            it.photo?.let(files::file)
        }
        backup.plans.forEach {
            require(it.vehicleId in vehicles && it.title.isNotBlank())
            bounded(it.title, 250)
            bounded(it.notes)
            require(it.intervalMonths == null || it.intervalMonths in 1..1200)
            require(it.warningDays in 0..3650)
            decimal(it.intervalKm)
            require(it.intervalKm == null || it.intervalKm.decimal().signum() > 0)
            decimal(it.dueKm)
            decimal(it.initialDueKm)
            decimal(it.warningKm)
            it.dueDate?.let(LocalDate::parse)
            it.initialDueDate?.let(LocalDate::parse)
            Currency.getInstance(it.currency)
            require(it.estimatedMinor == null || it.estimatedMinor >= 0)
        }
        backup.records.forEach {
            require(it.vehicleId in vehicles && it.amountMinor in 0..1_000_000_000_000_000L)
            bounded(it.title, 250)
            bounded(it.notes)
            bounded(it.stationOrWorkshop, 250)
            Instant.ofEpochMilli(it.timestamp)
            require(
                it.inputDistanceUnit in setOf("km", "mi") &&
                    it.inputVolumeUnit in setOf("L", "US gal")
            )
            Currency.getInstance(it.currency)
            decimal(it.odometerKm)
            decimal(it.liters)
            decimal(it.estimatedConsumptionLPer100Km)
            it.estimateAnchorRecordId?.let { anchor ->
                require(anchor.length == 36)
                java.util.UUID.fromString(anchor)
                records[anchor]?.let { reading ->
                    require(reading.vehicleId == it.vehicleId && reading.odometerKm != null)
                }
                require(it.estimatedConsumptionLPer100Km != null)
            }
            it.estimatedConsumptionLPer100Km?.let { assumed ->
                require(it.kind == RecordKind.FUEL && it.odometerKm == null)
                require(
                    assumed.decimal() in java.math.BigDecimal("0.1")..java.math.BigDecimal("1000")
                )
            }
            it.serviceDate?.let(LocalDate::parse)
            require(it.planId == null || plans[it.planId]?.vehicleId == it.vehicleId)
            if (it.kind == RecordKind.FUEL) {
                require(
                    it.fuelType != null &&
                        it.liters != null &&
                        it.liters.decimal().signum() > 0
                )
            }
            if (it.kind == RecordKind.SERVICE) {
                require(it.serviceDate != null && it.odometerKm != null)
            }
        }
        backup.records
            .filter {
                it.odometerKm != null
            }
            .groupBy {
                it.vehicleId
            }
            .values
            .forEach { list ->
                list
                    .sortedBy {
                        it.timestamp
                    }
                    .zipWithNext()
                    .forEach { (a, b) ->
                        require(b.odometerKm!!.decimal() >= a.odometerKm!!.decimal())
                        if (a.timestamp == b.timestamp) {
                            require(a.odometerKm.decimal().compareTo(b.odometerKm.decimal()) == 0)
                        }
                    }
            }
        backup.records
            .groupBy {
                it.currency
            }
            .values
            .forEach { items ->
                items.fold(0L) { total, record ->
                    Math.addExact(total, record.amountMinor)
                }
            }
        backup.attachments.forEach {
            require(
                it.recordId in records &&
                    it.mimeType in setOf("image/jpeg", "image/png", "image/webp", "application/pdf")
            )
            files.file(it.path)
        }
        backup.conversations.forEach {
            require(
                it.vehicleId in vehicles &&
                    it.language in setOf("en", "ru", "uk") &&
                    it.adviceJson.length <= 40_000
            )
            bounded(it.question, 3000)
            bounded(it.contextSnapshot, 40_000)
            val advice = Json.decodeFromString<VehicleAdvice>(it.adviceJson)
            require(
                advice.language in setOf("en", "ru", "uk") &&
                    advice.items.size <= 12 &&
                    advice.draftActions.size <= 8
            )
            require(
                advice.items.all { item ->
                    item.evidenceStatus in
                        setOf("generalGuidance", "suppliedUserFact", "requiresVerification")
                }
            )
        }
    }

    suspend fun csv(uri: Uri) = withContext(Dispatchers.IO) {
        val snapshot = repository.snapshot.first()
        val prefs = preferences.preferences.first()
        val zone = ZoneId.of(prefs.reportingZone)
        fun quote(value: String): String {
            val safe =
                if (
                    value.trimStart().startsWith("=") ||
                    value.trimStart().startsWith("+") ||
                    value.trimStart().startsWith("-") ||
                    value.trimStart().startsWith("@")
                ) {
                    "'$value"
                } else {
                    value
                }
            return "\"${safe.replace("\"", "\"\"")
            }\""
        }
        context.contentResolver
            .openOutputStream(uri, "wt")!!
            .bufferedWriter(Charsets.UTF_8)
            .use { writer ->
                writer.write("\uFEFF")
                writer.appendLine(
                    listOf(
                        R.string.garage,
                        R.string.date,
                        R.string.record_type,
                        R.string.title,
                        R.string.odometer,
                        R.string.volume,
                        R.string.amount,
                        R.string.currency,
                        R.string.notes,
                        R.string.assumed_consumption,
                        R.string.estimated_distance_column
                    )
                        .joinToString(",") {
                            quote(context.getString(it))
                        }
                )
                snapshot.records
                    .filter {
                        it.kind in
                            listOf(RecordKind.FUEL, RecordKind.SERVICE, RecordKind.EXPENSE)
                    }
                    .forEach { record ->
                        writer.appendLine(
                            listOf(
                                snapshot.vehicles
                                    .find {
                                        it.id == record.vehicleId
                                    }!!
                                    .displayName,
                                record.serviceDate
                                    ?: Instant.ofEpochMilli(record.timestamp)
                                        .atZone(zone)
                                        .toString(),
                                context.getString(kindLabel(record.kind)),
                                record.title,
                                record.odometerKm.orEmpty() + " km",
                                record.liters.orEmpty() + " L",
                                Money.major(record.amountMinor, record.currency)
                                    .toPlainString(),
                                record.currency,
                                record.notes,
                                record.estimatedConsumptionLPer100Km.orEmpty(),
                                record.estimatedConsumptionLPer100Km?.let { assumed ->
                                    FuelMileage.distanceKm(
                                        record.liters!!.decimal(),
                                        assumed.decimal()
                                    ).stripTrailingZeros().toPlainString()
                                }.orEmpty()
                            )
                                .joinToString(",", transform = ::quote)
                        )
                    }
            }
    }

    suspend fun pdf(uri: Uri, vehicle: Vehicle, range: DateRange, currency: String) =
        withContext(Dispatchers.IO) {
            require(billing.state.value.pro)
            val snapshot = repository.snapshot.first()
            val prefs = preferences.preferences.first()
            val stats =
                StatisticsCalculator.calculate(
                    snapshot.records.filter {
                        it.vehicleId == vehicle.id
                    },
                    vehicle.fuelType,
                    range,
                    ZoneId.of(prefs.reportingZone)
                )
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 11f
                    typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                }
            val title = context.getString(R.string.report_title)
            val document = PdfDocument()
            try {
                var pageNumber = 1
                var page =
                    document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                var y = 44f
                fun startPage() {
                    page.canvas.drawText(
                        context.getString(R.string.page_number, pageNumber),
                        44f,
                        810f,
                        paint
                    )
                    document.finishPage(page)
                    page =
                        document.startPage(
                            PdfDocument.PageInfo.Builder(595, 842, ++pageNumber).create()
                        )
                    y = 44f
                    page.canvas.drawText(title, 44f, y, paint)
                    y += 24f
                }
                fun line(text: String) {
                    text.split('\n').forEach { paragraph ->
                        var remaining = paragraph
                        if (remaining.isEmpty()) y += 14f
                        while (remaining.isNotEmpty()) {
                            if (y > 775f) startPage()
                            var count =
                                paint.breakText(remaining, true, 507f, null).coerceAtLeast(1)
                            if (count < remaining.length) {
                                val space = remaining.lastIndexOf(' ', count - 1)
                                if (space > 0) count = space + 1
                            }
                            page.canvas.drawText(remaining.take(count), 44f, y, paint)
                            remaining = remaining.drop(count)
                            y += 16f
                        }
                    }
                    y += 4f
                }
                line(title)
                line("${vehicle.make} ${vehicle.model} · ${vehicle.year} · ${vehicle.displayName}")
                line(
                    context.getString(
                        R.string.report_period,
                        range.start?.toString() ?: "…",
                        range.endInclusive?.toString() ?: "…"
                    )
                )
                line(context.getString(R.string.report_currency, currency))
                stats.totals
                    .find {
                        it.currency == currency
                    }
                    ?.let {
                        line(
                            context.getString(R.string.fuel_spending) +
                                ": " +
                                formatMoney(it.fuelMinor, currency)
                        )
                        line(
                            context.getString(R.string.service) +
                                ": " +
                                formatMoney(it.serviceMinor, currency)
                        )
                        line(
                            context.getString(R.string.expense) +
                                ": " +
                                formatMoney(it.otherMinor, currency)
                        )
                        line(
                            context.getString(R.string.ownership_total) +
                                ": " +
                                formatMoney(it.totalMinor, currency)
                        )
                    }
                line(context.getString(R.string.consumption_method))
                line(
                    stats.consumption.litersPer100Km?.let {
                        context.getString(
                            R.string.consumption_value,
                            it.stripTrailingZeros().toPlainString()
                        )
                    } ?: context.getString(R.string.not_enough_data)
                )
                line(context.getString(R.string.planned_cost_help))
                line(context.getString(R.string.journal))
                stats.records
                    .filter {
                        it.currency == currency &&
                            it.kind in
                            listOf(RecordKind.FUEL, RecordKind.SERVICE, RecordKind.EXPENSE)
                    }
                    .forEach {
                        line(
                            "${it.date(
                                ZoneId.of(prefs.reportingZone)
                            )} · ${context.getString(kindLabel(it.kind))} · ${it.title}"
                        )
                        line(
                            "${it.odometerKm.orEmpty()} km · ${formatMoney(
                                it.amountMinor,
                                currency
                            )} · ${it.stationOrWorkshop}"
                        )
                        it.estimatedConsumptionLPer100Km?.let { assumed ->
                            line(context.getString(R.string.assumed_consumption_value, assumed))
                            line(context.getString(R.string.estimated_mileage_warning))
                        }
                        if (it.notes.isNotBlank()) line(it.notes)
                    }
                page.canvas.drawText(
                    context.getString(R.string.page_number, pageNumber),
                    44f,
                    810f,
                    paint
                )
                document.finishPage(page)
                context.contentResolver.openOutputStream(uri, "wt")!!.use(document::writeTo)
            } finally {
                document.close()
            }
        }

    private fun formatMoney(amount: Long, currency: String): String =
        NumberFormat.getCurrencyInstance(context.resources.configuration.locales[0])
            .apply {
                this.currency = Currency.getInstance(currency)
            }
            .format(Money.major(amount, currency))
}

fun kindLabel(kind: RecordKind): Int = when (kind) {
    RecordKind.FUEL -> R.string.fuel
    RecordKind.SERVICE -> R.string.service
    RecordKind.EXPENSE -> R.string.expense
    RecordKind.ODOMETER -> R.string.odometer
    RecordKind.NOTE -> R.string.note
}
