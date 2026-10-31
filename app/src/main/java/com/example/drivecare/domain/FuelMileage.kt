package com.example.drivecare.domain

import com.example.drivecare.core.model.JournalRecord
import com.example.drivecare.core.model.RecordKind
import com.example.drivecare.core.model.decimal
import java.math.BigDecimal
import java.math.RoundingMode

data class MileageEstimate(
    val odometerKm: BigDecimal,
    val addedKm: BigDecimal,
    val fillCount: Int,
    val complete: Boolean
)

object FuelMileage {
    fun distanceKm(liters: BigDecimal, consumptionLPer100Km: BigDecimal): BigDecimal {
        require(liters.signum() > 0 && liters <= BigDecimal("1000000000"))
        require(consumptionLPer100Km in BigDecimal("0.1")..BigDecimal("1000"))
        return liters.multiply(
            BigDecimal("100")
        ).divide(consumptionLPer100Km, 6, RoundingMode.HALF_UP)
    }

    fun calculate(records: List<JournalRecord>): MileageEstimate? {
        val anchor = latestReading(records) ?: return null
        val fills =
            records.filter {
                it.kind == RecordKind.FUEL && (
                    it.timestamp > anchor.timestamp || (
                        it.timestamp == anchor.timestamp && it.estimateAnchorRecordId == anchor.id
                        )
                    )
            }
        var added = BigDecimal.ZERO
        var count = 0
        var complete = true
        fills.forEach { record ->
            val liters = record.liters?.toBigDecimalOrNull()
            val consumption = record.estimatedConsumptionLPer100Km?.toBigDecimalOrNull()
            if (liters == null || consumption == null) {
                complete = false
            } else {
                added += distanceKm(liters, consumption)
                count++
            }
            if (record.missingFill) complete = false
        }
        return if (count == 0) {
            null
        } else {
            MileageEstimate(
                anchor.odometerKm!!.decimal() + added,
                added,
                count,
                complete
            )
        }
    }
}
