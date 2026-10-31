package com.example.drivecare.feature.editor

import com.example.drivecare.core.model.*
import com.example.drivecare.data.billing.BillingRepository
import com.example.drivecare.data.local.PreferencesRepository
import com.example.drivecare.data.repository.DriveCareRepository
import com.example.drivecare.domain.Money
import com.example.drivecare.domain.Units
import com.example.drivecare.domain.latestReading
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Currency
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class EditorRepository
@Inject
constructor(
    private val repository: DriveCareRepository,
    private val preferences: PreferencesRepository,
    private val billing: BillingRepository
) {

    suspend fun initial(
        mode: String,
        id: String,
        vehicleId: String,
        planId: String
    ): Map<String, String> {
        val snapshot = repository.snapshot.first()
        val prefs = preferences.preferences.first()
        val zone = ZoneId.of(prefs.reportingZone)
        val vehicle =
            snapshot.vehicles.find {
                it.id == vehicleId
            }
        val record =
            snapshot.records.find {
                it.id == id
            }
        val plan =
            snapshot.plans.find {
                it.id == if (mode == "plan") id else planId
            }
        val now = Instant.now().atZone(zone)
        val values =
            linkedMapOf(
                "date" to now.toLocalDate().toString(),
                "time" to now.toLocalTime().withSecond(0).withNano(0).toString(),
                "currency" to
                    (record?.currency ?: plan?.currency ?: vehicle?.currency ?: prefs.currency),
                "distanceUnit" to (record?.inputDistanceUnit ?: prefs.distanceUnit),
                "volumeUnit" to (record?.inputVolumeUnit ?: prefs.volumeUnit),
                "fuelType" to (record?.fuelType ?: vehicle?.fuelType ?: FuelType.PETROL).name,
                "category" to (record?.category ?: plan?.category ?: Category.ENGINE_OIL).name,
                "warningDays" to "14",
                "warningKm" to "500",
                "reminders" to "false",
                "today" to now.toLocalDate().toString(),
                "estimateMileage" to (record?.estimatedConsumptionLPer100Km != null).toString(),
                "estimatedConsumption" to
                    (
                        record?.estimatedConsumptionLPer100Km
                            ?: snapshot.records.filter {
                                it.vehicleId == vehicleId &&
                                    it.kind == RecordKind.FUEL
                            }
                                .sortedByDescending { it.timestamp }
                                .firstNotNullOfOrNull { it.estimatedConsumptionLPer100Km }
                        ).orEmpty(),
                "recordMileage" to (record?.odometerKm != null).toString(),
                "repeatPlan" to
                    (plan?.intervalMonths != null || plan?.intervalKm != null).toString(),
                "dueMode" to
                    when {
                        plan?.initialDueDate != null && plan.initialDueKm != null -> "both"
                        plan?.initialDueKm != null -> "distance"
                        else -> "date"
                    }
            )
        val reading = latestReading(snapshot.records.filter { it.vehicleId == vehicleId })
        reading?.odometerKm?.let { km ->
            val display =
                Units.fromKm(
                    km.decimal(),
                    values.getValue("distanceUnit")
                ).stripTrailingZeros().toPlainString()
            values["odometerBaseline"] = display
            values["canonical.odometerBaseline"] = km
            if (record == null && mode in listOf("fuel", "service", "odometer")) {
                values["odometer"] = display
                values["canonical.odometer"] = km
            }
        }
        if (mode == "vehicle") {
            val old =
                snapshot.vehicles.find {
                    it.id == id
                }
            if (old != null) {
                values.putAll(
                    mapOf(
                        "make" to old.make,
                        "model" to old.model,
                        "year" to old.year.toString(),
                        "nickname" to old.nickname,
                        "generation" to old.generation,
                        "engine" to old.engine,
                        "fuelType" to old.fuelType.name,
                        "transmission" to old.transmission,
                        "market" to old.market,
                        "currency" to old.currency,
                        "purchaseDate" to old.purchaseDate.orEmpty(),
                        "purchaseMileage" to
                            old.purchaseOdometerKm
                                ?.let {
                                    Units.fromKm(it.decimal(), prefs.distanceUnit).toPlainString()
                                }
                                .orEmpty(),
                        "photo" to old.photo.orEmpty()
                    )
                )
            }
        } else if (mode == "plan" && plan != null) {
            values.putAll(
                mapOf(
                    "title" to plan.title,
                    "notes" to plan.notes,
                    "dueDate" to plan.initialDueDate.orEmpty(),
                    "dueKm" to
                        plan.initialDueKm
                            ?.let {
                                Units.fromKm(it.decimal(), prefs.distanceUnit).toPlainString()
                            }
                            .orEmpty(),
                    "intervalMonths" to plan.intervalMonths?.toString().orEmpty(),
                    "intervalKm" to
                        plan.intervalKm
                            ?.let {
                                Units.fromKm(it.decimal(), prefs.distanceUnit).toPlainString()
                            }
                            .orEmpty(),
                    "warningDays" to plan.warningDays.toString(),
                    "warningKm" to
                        Units.fromKm(plan.warningKm.decimal(), prefs.distanceUnit).toPlainString(),
                    "estimated" to
                        plan.estimatedMinor
                            ?.let {
                                Money.major(it, plan.currency).toPlainString()
                            }
                            .orEmpty(),
                    "reminders" to plan.reminders.toString()
                )
            )
        } else if (record != null) {
            val timestamp = Instant.ofEpochMilli(record.timestamp).atZone(zone)
            values.putAll(
                mapOf(
                    "title" to record.title,
                    "notes" to record.notes,
                    "date" to (record.serviceDate ?: timestamp.toLocalDate().toString()),
                    "time" to timestamp.toLocalTime().withNano(0).toString(),
                    "odometer" to
                        record.odometerKm
                            ?.let {
                                Units.fromKm(it.decimal(), record.inputDistanceUnit).toPlainString()
                            }
                            .orEmpty(),
                    "volume" to
                        record.liters
                            ?.let {
                                Units.fromLiters(it.decimal(), record.inputVolumeUnit)
                                    .toPlainString()
                            }
                            .orEmpty(),
                    "amount" to Money.major(record.amountMinor, record.currency).toPlainString(),
                    "fullTank" to record.fullTank.toString(),
                    "missingFill" to record.missingFill.toString(),
                    "station" to record.stationOrWorkshop,
                    "attachments" to
                        snapshot.attachments
                            .filter {
                                it.recordId == id
                            }
                            .joinToString("|") {
                                "${it.path};${it.mimeType}"
                            }
                )
            )
        } else if (mode == "service" && plan != null) {
            values["title"] = plan.title
            values["notes"] = plan.notes
            values["category"] = plan.category.name
        }
        if (mode == "fuel" && record?.liters != null) {
            val inputVolume = Units.fromLiters(record.liters.decimal(), record.inputVolumeUnit)
            if (inputVolume.signum() > 0) {
                values["unitPrice"] =
                    Money.major(record.amountMinor, record.currency)
                        .divide(inputVolume, 6, RoundingMode.HALF_UP)
                        .stripTrailingZeros().toPlainString()
            }
        }
        fun retain(key: String, canonical: String?) {
            canonical?.let { values["canonical.$key"] = it }
        }
        if (mode == "vehicle") {
            retain("purchaseMileage", snapshot.vehicles.find { it.id == id }?.purchaseOdometerKm)
        } else if (mode == "plan" && plan != null) {
            retain("dueKm", plan.initialDueKm)
            retain("intervalKm", plan.intervalKm)
            retain("warningKm", plan.warningKm)
        } else if (record != null) {
            retain("odometer", record.odometerKm)
            retain("volume", record.liters)
        }
        return values
    }

    suspend fun save(
        mode: String,
        id: String,
        vehicleId: String,
        planId: String,
        values: Map<String, String>
    ): String {
        val snapshot = repository.snapshot.first()
        val prefs = preferences.preferences.first()
        val zone = ZoneId.of(prefs.reportingZone)
        fun text(key: String): String = values[key].orEmpty().let {
            if (key in setOf("title", "notes", "nickname")) it else it.trim()
        }.also {
            require(it.length <= if (key in setOf("notes", "attachments")) 10_000 else 250)
        }
        fun optional(key: String): String? = text(key).takeIf {
            it.isNotEmpty()
        }
        fun decimal(key: String): BigDecimal = parseDecimal(text(key)).also {
            require(it.signum() >= 0 && it <= BigDecimal("1000000000"))
        }
        fun km(key: String): String? = optional(key)?.let {
            values["canonical.$key"]
                ?: Units.toKm(decimal(key), text("distanceUnit")).toPlainString()
        }
        fun date(key: String): String? = optional(key)?.let {
            LocalDate.parse(it).toString()
        }
        val currency = text("currency").uppercase()
        Currency.getInstance(currency)
        val existingVehicle =
            snapshot.vehicles.find {
                it.id == if (mode == "vehicle") id else vehicleId
            }
        val existingRecord =
            snapshot.records.find {
                it.id == id
            }
        val savedId =
            id.ifBlank {
                newId()
            }
        if (mode == "vehicle") {
            val make = text("make")
            val model = text("model")
            require(make.isNotBlank() && model.isNotBlank())
            val year = text("year").toInt()
            require(year in 1886..LocalDate.now(zone).year + 1)
            val vehicle =
                Vehicle(
                    id = savedId,
                    nickname = text("nickname"),
                    make = make,
                    model = model,
                    year = year,
                    generation = text("generation"),
                    engine = text("engine"),
                    fuelType = FuelType.valueOf(text("fuelType")),
                    transmission = text("transmission"),
                    market = text("market"),
                    currency = currency,
                    purchaseDate = date("purchaseDate"),
                    purchaseOdometerKm = km("purchaseMileage"),
                    photo = optional("photo"),
                    archived = existingVehicle?.archived ?: false,
                    revision = (existingVehicle?.revision ?: 0) + 1
                )
            val initial =
                if (existingVehicle == null) {
                    val day = LocalDate.parse(text("date"))
                    require(!day.isAfter(LocalDate.now(zone)))
                    val reading = km("odometer") ?: error("required")
                    require(
                        vehicle.purchaseDate == null || LocalDate.parse(vehicle.purchaseDate) <= day
                    )
                    require(
                        vehicle.purchaseOdometerKm == null ||
                            vehicle.purchaseOdometerKm.decimal() <= reading.decimal()
                    )
                    JournalRecord(
                        vehicleId = savedId,
                        kind = RecordKind.ODOMETER,
                        currency = currency,
                        timestamp = day.atStartOfDay(zone).toInstant().toEpochMilli(),
                        odometerKm = reading,
                        inputDistanceUnit = text("distanceUnit")
                    )
                } else {
                    null
                }
            repository.saveVehicle(vehicle, initial, billing.state.value.pro)
            preferences.setString("vehicle", savedId)
        } else if (mode == "plan") {
            val old =
                snapshot.plans.find {
                    it.id == id
                }
            val title = text("title")
            require(title.isNotBlank())
            val dueMode = text("dueMode").ifBlank { "date" }
            val dueDate = if (dueMode == "distance") null else date("dueDate")
            val dueKm = if (dueMode == "date") null else km("dueKm")
            require(dueMode != "both" || (dueDate != null && dueKm != null))
            require(dueDate != null || dueKm != null)
            val recurring = text("repeatPlan").toBoolean()
            val months =
                if (!recurring ||
                    dueMode == "distance"
                ) {
                    null
                } else {
                    optional("intervalMonths")?.toInt()?.also {
                        require(it in 1..1200)
                    }
                }
            val interval =
                if (!recurring ||
                    dueMode == "date"
                ) {
                    null
                } else {
                    km("intervalKm")?.also {
                        require(it.decimal().signum() > 0)
                    }
                }
            require(!recurring || months != null || interval != null)
            val days = text("warningDays").toInt().also { require(it in 0..3650) }
            val warning = km("warningKm") ?: "0"
            val plan =
                MaintenancePlan(
                    id = savedId,
                    vehicleId = vehicleId,
                    title = title,
                    category = Category.valueOf(text("category")),
                    notes = text("notes"),
                    intervalMonths = months,
                    intervalKm = interval,
                    initialDueDate = dueDate,
                    initialDueKm = dueKm,
                    warningDays = days,
                    warningKm = warning,
                    estimatedMinor =
                        optional("estimated")?.let {
                            Money.minor(decimal("estimated"), currency)
                        },
                    currency = currency,
                    reminders = text("reminders").toBoolean(),
                    paused = old?.paused ?: false,
                    lastNotifiedDay = old?.lastNotifiedDay
                )
            repository.savePlan(plan)
        } else {
            val kind =
                when (mode) {
                    "fuel" -> RecordKind.FUEL
                    "service" -> RecordKind.SERVICE
                    "expense" -> RecordKind.EXPENSE
                    "odometer" -> RecordKind.ODOMETER
                    else -> RecordKind.NOTE
                }
            val day = LocalDate.parse(text("date"))
            val time =
                LocalTime.parse(
                    text("time").ifBlank {
                        "12:00"
                    }
                )
            val timestamp =
                if (kind == RecordKind.SERVICE) {
                    day.atTime(12, 0).atZone(zone).toInstant()
                } else {
                    day.atTime(time).atZone(zone).toInstant()
                }
            require(
                if (kind == RecordKind.SERVICE) {
                    day <= LocalDate.now(zone)
                } else {
                    timestamp <= Instant.now().plusSeconds(60)
                }
            )
            val odometer =
                if (kind == RecordKind.FUEL &&
                    !text("recordMileage").toBoolean()
                ) {
                    null
                } else {
                    km("odometer")
                }
            if (kind in listOf(RecordKind.SERVICE, RecordKind.ODOMETER) ||
                (kind == RecordKind.FUEL && text("recordMileage").toBoolean())
            ) {
                require(odometer != null)
            }
            val amount =
                if (kind in listOf(RecordKind.FUEL, RecordKind.SERVICE, RecordKind.EXPENSE)) {
                    Money.minor(decimal("amount"), currency)
                } else {
                    0
                }
            val liters =
                if (kind == RecordKind.FUEL) {
                    (
                        values["canonical.volume"]?.decimal()
                            ?: Units.toLiters(decimal("volume"), text("volumeUnit"))
                        )
                        .also {
                            require(it.signum() > 0)
                        }
                        .toPlainString()
                } else {
                    null
                }
            if (kind in listOf(RecordKind.SERVICE, RecordKind.EXPENSE, RecordKind.NOTE)) {
                require(text("title").isNotBlank())
            }
            val record =
                JournalRecord(
                    id = savedId,
                    vehicleId = vehicleId,
                    kind = kind,
                    timestamp = timestamp.toEpochMilli(),
                    serviceDate = if (kind == RecordKind.SERVICE) day.toString() else null,
                    odometerKm = odometer,
                    inputDistanceUnit = text("distanceUnit"),
                    inputVolumeUnit = text("volumeUnit"),
                    title = text("title"),
                    notes = text("notes"),
                    category = Category.valueOf(text("category")),
                    amountMinor = amount,
                    currency = currency,
                    liters = liters,
                    fuelType =
                        if (kind == RecordKind.FUEL) FuelType.valueOf(text("fuelType")) else null,
                    fullTank = text("fullTank").toBoolean(),
                    missingFill = text("missingFill").toBoolean(),
                    stationOrWorkshop = text("station"),
                    estimateAnchorRecordId =
                        if (kind == RecordKind.FUEL && text("estimateMileage").toBoolean()) {
                            if (existingRecord?.estimatedConsumptionLPer100Km != null &&
                                existingRecord.timestamp == timestamp.toEpochMilli()
                            ) {
                                existingRecord.estimateAnchorRecordId
                            } else {
                                latestReading(
                                    snapshot.records.filter {
                                        it.vehicleId == vehicleId && it.id != savedId &&
                                            it.timestamp <= timestamp.toEpochMilli()
                                    }
                                )?.id
                            }
                        } else {
                            null
                        },
                    estimatedConsumptionLPer100Km =
                        if (kind == RecordKind.FUEL && text("estimateMileage").toBoolean()) {
                            require(odometer == null)
                            decimal("estimatedConsumption").also {
                                require(it >= BigDecimal("0.1") && it <= BigDecimal("1000"))
                            }.stripTrailingZeros().toPlainString()
                        } else {
                            null
                        },
                    planId =
                        existingRecord?.planId ?: planId.takeIf {
                            it.isNotBlank()
                        }
                )
            val attachments =
                text("attachments")
                    .split('|')
                    .filter {
                        it.isNotBlank()
                    }
                    .map { entry ->
                        val (path, mime) = entry.split(';')
                        snapshot.attachments.find {
                            it.recordId == savedId && it.path == path
                        }
                            ?: Attachment(recordId = savedId, path = path, mimeType = mime)
                    }
            repository.saveRecord(record, attachments)
        }
        return savedId
    }
}

fun parseDecimal(value: String): BigDecimal {
    require(value.length <= 24 && value.matches(Regex("[0-9]+([.,][0-9]+)?")))
    return value.replace(',', '.').toBigDecimal()
}
