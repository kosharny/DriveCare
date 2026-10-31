package com.example.drivecare.domain

import com.example.drivecare.core.model.*
import java.time.ZoneId

data class CategoryTotal(val category: Category, val currency: String, val minor: Long)

data class NamedTotal(val name: String, val currency: String, val minor: Long)

data class AdvancedStatistics(
    val previousRange: DateRange?,
    val previousTotals: List<CurrencyTotals>,
    val categories: List<CategoryTotal>,
    val places: List<NamedTotal>,
    val vehicles: List<NamedTotal>
)

object AdvancedStatisticsCalculator {

    fun calculate(
        snapshot: StoreSnapshot,
        vehicle: Vehicle,
        stats: Statistics,
        range: DateRange,
        zone: ZoneId
    ): AdvancedStatistics {
        val previousRange =
            if (range.start != null && range.endInclusive != null) {
                val days =
                    java.time.temporal.ChronoUnit.DAYS.between(range.start, range.endInclusive) + 1
                DateRange(range.start.minusDays(days), range.start.minusDays(1))
            } else {
                null
            }
        val previousTotals =
            previousRange
                ?.let {
                    StatisticsCalculator.calculate(
                        snapshot.records.filter {
                            it.vehicleId == vehicle.id
                        },
                        vehicle.fuelType,
                        it,
                        zone
                    )
                        .totals
                }
                .orEmpty()
        val categories =
            stats.records
                .filter {
                    it.kind in listOf(RecordKind.EXPENSE, RecordKind.SERVICE)
                }
                .groupBy {
                    it.category to it.currency
                }
                .map { (key, records) ->
                    CategoryTotal(
                        key.first,
                        key.second,
                        records.fold(0L) { sum, record ->
                            Math.addExact(sum, record.amountMinor)
                        }
                    )
                }
        val places =
            stats.records
                .filter {
                    it.stationOrWorkshop.isNotBlank() &&
                        it.kind in listOf(RecordKind.FUEL, RecordKind.SERVICE, RecordKind.EXPENSE)
                }
                .groupBy {
                    it.stationOrWorkshop to it.currency
                }
                .map { (key, records) ->
                    NamedTotal(
                        key.first,
                        key.second,
                        records.fold(0L) { sum, record ->
                            Math.addExact(sum, record.amountMinor)
                        }
                    )
                }
        val vehicles =
            snapshot.vehicles.flatMap { item ->
                StatisticsCalculator.calculate(
                    snapshot.records.filter {
                        it.vehicleId == item.id
                    },
                    item.fuelType,
                    range,
                    zone
                )
                    .totals
                    .map {
                        NamedTotal(item.displayName, it.currency, it.totalMinor)
                    }
            }
        return AdvancedStatistics(previousRange, previousTotals, categories, places, vehicles)
    }
}
