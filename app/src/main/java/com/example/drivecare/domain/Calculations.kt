package com.example.drivecare.domain

import com.example.drivecare.core.model.FuelType
import com.example.drivecare.core.model.JournalRecord
import com.example.drivecare.core.model.MaintenancePlan
import com.example.drivecare.core.model.RecordKind
import com.example.drivecare.core.model.date
import com.example.drivecare.core.model.decimal
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Currency

object Units {

    private val kmPerMile = BigDecimal("1.609344")
    private val litersPerGallon = BigDecimal("3.785411784")

    fun toKm(value: BigDecimal, unit: String): BigDecimal =
        if (unit == "mi") value.multiply(kmPerMile) else value

    fun fromKm(value: BigDecimal, unit: String): BigDecimal =
        if (unit == "mi") value.divide(kmPerMile, 8, RoundingMode.HALF_UP) else value

    fun toLiters(value: BigDecimal, unit: String): BigDecimal =
        if (unit == "US gal") value.multiply(litersPerGallon) else value

    fun fromLiters(value: BigDecimal, unit: String): BigDecimal =
        if (unit == "US gal") value.divide(litersPerGallon, 8, RoundingMode.HALF_UP) else value
}

object Money {

    fun scale(currency: String): Int =
        Currency.getInstance(currency).defaultFractionDigits.coerceAtLeast(0)

    fun minor(value: BigDecimal, currency: String): Long = value
        .setScale(scale(currency), RoundingMode.HALF_UP)
        .movePointRight(scale(currency))
        .longValueExact()

    fun major(minor: Long, currency: String): BigDecimal =
        BigDecimal.valueOf(minor, scale(currency))
}

data class DateRange(val start: LocalDate?, val endInclusive: LocalDate?) {

    init {

        require(start == null || endInclusive == null || !start.isAfter(endInclusive))
    }

    fun contains(record: JournalRecord, zone: ZoneId): Boolean {
        val instant = Instant.ofEpochMilli(record.timestamp)
        return if (record.kind == RecordKind.SERVICE) {
            val date = record.date(zone)
            (start == null || !date.isBefore(start)) &&
                (endInclusive == null || !date.isAfter(endInclusive))
        } else {
            (start == null || instant >= start.atStartOfDay(zone).toInstant()) &&
                (
                    endInclusive == null ||
                        instant < endInclusive.plusDays(1).atStartOfDay(zone).toInstant()
                    )
        }
    }
}

data class ConsumptionInterval(
    val endTimestamp: Long,
    val distanceKm: BigDecimal,
    val liters: BigDecimal
)

data class ConsumptionResult(
    val intervals: List<ConsumptionInterval>,
    val excludedIntervals: Int,
    val unsupportedDualFuel: Boolean
) {

    val litersPer100Km: BigDecimal?
        get() {

            val distance =
                intervals.fold(BigDecimal.ZERO) { sum, item ->
                    sum + item.distanceKm
                }
            if (distance.signum() <= 0) return null
            val liters =
                intervals.fold(BigDecimal.ZERO) { sum, item ->
                    sum + item.liters
                }
            return liters.multiply(BigDecimal(100)).divide(distance, 4, RoundingMode.HALF_UP)
        }
}

object FuelConsumption {

    fun calculate(
        records: List<JournalRecord>,
        powertrain: FuelType,
        range: DateRange,
        zone: ZoneId
    ): ConsumptionResult {
        val fuels =
            records.filter {
                it.kind == RecordKind.FUEL
            }.sortedBy {
                it.timestamp
            }
        val streams =
            fuels.map {
                it.fuelType
            }.distinct()
        if (powertrain == FuelType.LPG || streams.size > 1) {
            return ConsumptionResult(emptyList(), 0, true)
        }
        val intervals = mutableListOf<ConsumptionInterval>()
        var baseline: JournalRecord? = null
        var last: JournalRecord? = null
        var volume = BigDecimal.ZERO
        var valid = true
        var excluded = 0
        val duplicateTimes =
            fuels.groupingBy {
                it.timestamp
            }.eachCount().filterValues {
                it > 1
            }.keys
        fuels.forEach { record ->
            val previous = last
            val km = record.odometerKm?.toBigDecimalOrNull()
            val previousKm = previous?.odometerKm?.toBigDecimalOrNull()
            if (baseline != null) {
                volume += record.liters?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                if (
                    record.missingFill ||
                    km == null ||
                    previousKm == null ||
                    km <= previousKm ||
                    record.timestamp in duplicateTimes ||
                    previous?.timestamp in duplicateTimes ||
                    record.fuelType != baseline?.fuelType
                ) {
                    valid = false
                }
            }
            if (record.fullTank) {
                val start = baseline
                if (start != null) {
                    val distance = (km ?: BigDecimal.ZERO) - start.odometerKm!!.decimal()
                    if (valid && distance.signum() > 0 && volume.signum() > 0) {
                        if (range.contains(record, zone)) {
                            intervals += ConsumptionInterval(record.timestamp, distance, volume)
                        }
                    } else if (range.contains(record, zone)) {
                        excluded++
                    }
                }
                baseline = if (km != null && record.timestamp !in duplicateTimes) record else null
                volume = BigDecimal.ZERO
                valid = true
            }
            last = record
        }
        return ConsumptionResult(intervals, excluded, false)
    }
}

enum class DueStatus {

    PLANNED,
    DUE_SOON,
    DUE,
    OVERDUE,
    PAUSED,
    COMPLETED
}

data class PlanStatus(
    val status: DueStatus,
    val dateTriggered: Boolean,
    val distanceTriggered: Boolean,
    val mileageStale: Boolean
)

object MaintenanceRules {

    fun recalculate(plan: MaintenancePlan, records: List<JournalRecord>): MaintenancePlan {
        val latest =
            records
                .filter {
                    it.kind == RecordKind.SERVICE && it.planId == plan.id
                }
                .maxWithOrNull(
                    compareBy<JournalRecord> {
                        it.serviceDate
                    }.thenBy {
                        it.timestamp
                    }
                )
        if (latest == null) {
            return plan.copy(
                dueDate = plan.initialDueDate,
                dueKm = plan.initialDueKm,
                completed = false,
                lastNotifiedDay = plan.lastNotifiedDay
            )
        }
        val recurring = plan.intervalMonths != null || plan.intervalKm != null
        return plan.copy(
            dueDate =
                plan.intervalMonths?.let { months ->
                    latest.serviceDate?.let {
                        LocalDate.parse(it).plusMonths(months.toLong()).toString()
                    }
                },
            dueKm =
                plan.intervalKm?.let { interval ->
                    latest.odometerKm?.decimal()?.plus(interval.decimal())?.toPlainString()
                },
            completed = !recurring,
            lastNotifiedDay = plan.lastNotifiedDay
        )
    }

    fun status(
        plan: MaintenancePlan,
        reading: JournalRecord?,
        today: LocalDate,
        zone: ZoneId
    ): PlanStatus {
        val date = plan.dueDate?.let(LocalDate::parse)
        val km = reading?.odometerKm?.decimal()
        val threshold = plan.dueKm?.decimal()
        val dateDiff =
            date?.let {
                java.time.temporal.ChronoUnit.DAYS.between(today, it)
            }
        val kmDiff = if (km != null && threshold != null) threshold - km else null
        val stale = reading == null || reading.date(zone).isBefore(today.minusDays(30))
        val dateReached = dateDiff != null && dateDiff <= 0
        val kmReached = kmDiff != null && kmDiff.signum() <= 0
        val status =
            when {
                plan.paused -> DueStatus.PAUSED

                plan.completed -> DueStatus.COMPLETED

                (dateDiff != null && dateDiff < 0) || (kmDiff != null && kmDiff.signum() < 0) ->
                    DueStatus.OVERDUE

                dateReached || kmReached -> DueStatus.DUE

                (dateDiff != null && dateDiff <= plan.warningDays) ||
                    (kmDiff != null && kmDiff <= plan.warningKm.decimal()) -> DueStatus.DUE_SOON

                else -> DueStatus.PLANNED
            }
        return PlanStatus(status, dateReached, kmReached, stale)
    }
}

fun latestReading(records: List<JournalRecord>): JournalRecord? = records.filter {
    it.odometerKm != null
}.maxByOrNull {
    it.timestamp
}

data class CurrencyTotals(
    val currency: String,
    val fuelMinor: Long,
    val serviceMinor: Long,
    val otherMinor: Long
) {

    val totalMinor: Long
        get() = Math.addExact(Math.addExact(fuelMinor, serviceMinor), otherMinor)
}

data class FuelGroup(
    val currency: String,
    val fuelType: FuelType,
    val count: Int,
    val liters: BigDecimal,
    val amountMinor: Long
) {

    val pricePerLiter: BigDecimal?
        get() =
            if (liters.signum() > 0) {

                Money.major(amountMinor, currency).divide(liters, 6, RoundingMode.HALF_UP)
            } else {
                null
            }
}

data class Statistics(
    val totals: List<CurrencyTotals>,
    val fuelGroups: List<FuelGroup>,
    val monthlyFuel: Map<String, Map<String, Long>>,
    val consumption: ConsumptionResult,
    val records: List<JournalRecord>
)

object StatisticsCalculator {

    fun calculate(
        records: List<JournalRecord>,
        fuelType: FuelType,
        range: DateRange,
        zone: ZoneId
    ): Statistics {
        val selected =
            records.filter {
                range.contains(it, zone)
            }
        fun List<JournalRecord>.sumMoney(): Long = fold(0L) { sum, item ->
            Math.addExact(sum, item.amountMinor)
        }
        val paid =
            selected.filter {
                it.kind in listOf(RecordKind.FUEL, RecordKind.SERVICE, RecordKind.EXPENSE)
            }
        val totals =
            paid
                .groupBy {
                    it.currency
                }
                .map { (currency, items) ->
                    CurrencyTotals(
                        currency,
                        items.filter {
                            it.kind == RecordKind.FUEL
                        }.sumMoney(),
                        items.filter {
                            it.kind == RecordKind.SERVICE
                        }.sumMoney(),
                        items.filter {
                            it.kind == RecordKind.EXPENSE
                        }.sumMoney()
                    )
                }
                .sortedBy {
                    it.currency
                }
        val fuel =
            selected.filter {
                it.kind == RecordKind.FUEL
            }
        val groups =
            fuel
                .groupBy {
                    it.currency to it.fuelType!!
                }
                .map { (key, items) ->
                    FuelGroup(
                        key.first,
                        key.second,
                        items.size,
                        items.fold(BigDecimal.ZERO) { sum, item ->
                            sum + item.liters!!.decimal()
                        },
                        items.sumMoney()
                    )
                }
        val monthly =
            fuel
                .groupBy {
                    it.date(zone).withDayOfMonth(1).toString()
                }
                .toSortedMap()
                .mapValues { (_, items) ->
                    items
                        .groupBy {
                            it.currency
                        }
                        .mapValues { (_, currencyItems) ->
                            currencyItems.sumMoney()
                        }
                }
        return Statistics(
            totals,
            groups,
            monthly,
            FuelConsumption.calculate(records, fuelType, range, zone),
            selected
        )
    }
}
