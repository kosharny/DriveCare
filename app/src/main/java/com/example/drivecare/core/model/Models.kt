package com.example.drivecare.core.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.Serializable

fun newId(): String = UUID.randomUUID().toString()

@Serializable
enum class FuelType {

    PETROL,
    DIESEL,
    LPG,
    PETROL_HYBRID,
    DIESEL_HYBRID,
    PLUG_IN_HYBRID
}

@Serializable
enum class RecordKind {

    FUEL,
    SERVICE,
    EXPENSE,
    ODOMETER,
    NOTE
}

@Serializable
enum class Category {

    ENGINE_OIL,
    OIL_FILTER,
    AIR_FILTER,
    CABIN_FILTER,
    TIRES,
    BRAKES,
    COOLANT,
    INSPECTION,
    REPAIR,
    PARKING,
    WASHING,
    INSURANCE,
    REGISTRATION,
    ACCESSORIES,
    OTHER
}

@Serializable
@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey val id: String = newId(),
    val nickname: String = "",
    val make: String,
    val model: String,
    val year: Int,
    val generation: String = "",
    val engine: String = "",
    val fuelType: FuelType,
    val transmission: String = "",
    val market: String = "",
    val currency: String,
    val purchaseDate: String? = null,
    val purchaseOdometerKm: String? = null,
    val photo: String? = null,
    val archived: Boolean = false,
    val revision: Long = 1
) {

    val displayName: String
        get() =
            nickname.ifBlank {
                "$make $model"
            }
}

@Serializable
@Entity(
    tableName = "plans",
    foreignKeys =
        [
            ForeignKey(
                entity = Vehicle::class,
                parentColumns = ["id"],
                childColumns = ["vehicleId"],
                onDelete = ForeignKey.CASCADE
            )
        ],
    indices = [Index("vehicleId")]
)
data class MaintenancePlan(
    @PrimaryKey val id: String = newId(),
    val vehicleId: String,
    val title: String,
    val category: Category,
    val notes: String = "",
    val intervalMonths: Int? = null,
    val intervalKm: String? = null,
    val initialDueDate: String? = null,
    val initialDueKm: String? = null,
    val dueDate: String? = initialDueDate,
    val dueKm: String? = initialDueKm,
    val warningDays: Int = 14,
    val warningKm: String = "500",
    val estimatedMinor: Long? = null,
    val currency: String,
    val reminders: Boolean = false,
    val paused: Boolean = false,
    val completed: Boolean = false,
    val lastNotifiedDay: String? = null
)

@Serializable
@Entity(
    tableName = "records",
    foreignKeys =
        [
            ForeignKey(
                entity = Vehicle::class,
                parentColumns = ["id"],
                childColumns = ["vehicleId"],
                onDelete = ForeignKey.CASCADE
            ),
            ForeignKey(
                entity = MaintenancePlan::class,
                parentColumns = ["id"],
                childColumns = ["planId"],
                onDelete = ForeignKey.SET_NULL
            )
        ],
    indices = [Index("vehicleId"), Index("planId"), Index(value = ["vehicleId", "timestamp"])]
)
data class JournalRecord(
    @PrimaryKey val id: String = newId(),
    val vehicleId: String,
    val kind: RecordKind,
    val timestamp: Long = Instant.now().toEpochMilli(),
    val serviceDate: String? = null,
    val odometerKm: String? = null,
    val inputDistanceUnit: String = "km",
    val title: String = "",
    val notes: String = "",
    val category: Category = Category.OTHER,
    val amountMinor: Long = 0,
    val currency: String,
    val liters: String? = null,
    val inputVolumeUnit: String = "L",
    val fuelType: FuelType? = null,
    val fullTank: Boolean = false,
    val missingFill: Boolean = false,
    val stationOrWorkshop: String = "",
    val planId: String? = null,
    val estimatedConsumptionLPer100Km: String? = null,
    val estimateAnchorRecordId: String? = null
)

@Serializable
@Entity(
    tableName = "attachments",
    foreignKeys =
        [
            ForeignKey(
                entity = JournalRecord::class,
                parentColumns = ["id"],
                childColumns = ["recordId"],
                onDelete = ForeignKey.CASCADE
            )
        ],
    indices = [Index("recordId")]
)
data class Attachment(
    @PrimaryKey val id: String = newId(),
    val recordId: String,
    val path: String,
    val mimeType: String
)

@Serializable
@Entity(
    tableName = "conversations",
    foreignKeys =
        [
            ForeignKey(
                entity = Vehicle::class,
                parentColumns = ["id"],
                childColumns = ["vehicleId"],
                onDelete = ForeignKey.CASCADE
            )
        ],
    indices = [Index("vehicleId")]
)
data class Conversation(
    @PrimaryKey val id: String = newId(),
    val vehicleId: String,
    val revision: Long,
    val timestamp: Long,
    val language: String,
    val question: String,
    val contextSnapshot: String,
    val adviceJson: String
)

@Serializable
data class AdviceItem(
    val title: String,
    val explanation: String,
    val signsToWatch: List<String>,
    val nextSteps: List<String>,
    val compatibilityChecks: List<String>,
    val evidenceStatus: String
)

@Serializable data class DraftAction(val title: String, val notes: String)

@Serializable
data class VehicleAdvice(
    val mode: String,
    val language: String,
    val summary: String,
    val missingDetails: List<String>,
    val items: List<AdviceItem>,
    val draftActions: List<DraftAction>
)

data class StoreSnapshot(
    val vehicles: List<Vehicle> = emptyList(),
    val records: List<JournalRecord> = emptyList(),
    val plans: List<MaintenancePlan> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val loaded: Boolean = false
)

data class AppPreferences(
    val loaded: Boolean = false,
    val onboardingDone: Boolean = false,
    val selectedVehicleId: String = "",
    val distanceUnit: String = "km",
    val volumeUnit: String = "L",
    val currency: String = "UAH",
    val theme: String = "system",
    val reportingZone: String = java.time.ZoneId.systemDefault().id,
    val aiConsent: Boolean = false,
    val aiDay: String = "",
    val aiCount: Int = 0,
    val aiLastSuccess: Long = 0
)

fun String.decimal(): BigDecimal = toBigDecimal()

fun JournalRecord.date(zone: java.time.ZoneId): LocalDate =
    serviceDate?.let(LocalDate::parse) ?: Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
