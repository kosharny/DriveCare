package com.example.drivecare.data.repository

import androidx.room.withTransaction
import com.example.drivecare.core.model.*
import com.example.drivecare.core.notifications.ReminderScheduler
import com.example.drivecare.data.local.AttachmentStore
import com.example.drivecare.data.local.DriveCareDatabase
import com.example.drivecare.domain.MaintenanceRules
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class DomainException(val reason: String) : IllegalArgumentException(reason)

@Singleton
class DriveCareRepository
@Inject
constructor(
    private val database: DriveCareDatabase,
    private val scheduler: ReminderScheduler,
    private val files: AttachmentStore
) {

    private val dao = database.dao()
    val snapshot =
        combine(
            dao.vehicles(),
            dao.records(),
            dao.plans(),
            dao.conversations(),
            dao.attachments()
        ) { vehicles, records, plans, conversations, attachments ->
            StoreSnapshot(vehicles, records, plans, conversations, attachments, loaded = true)
        }

    suspend fun saveVehicle(vehicle: Vehicle, initial: JournalRecord?, pro: Boolean) {
        val oldPhoto = dao.vehicle(vehicle.id)?.photo
        database.withTransaction {
            val existing = dao.vehicle(vehicle.id)
            if (
                !vehicle.archived &&
                (existing == null || existing.archived) &&
                !pro &&
                dao.activeVehicles().any {
                    it.id != vehicle.id
                }
            ) {
                throw DomainException("pro_required")
            }
            dao.putVehicle(vehicle)
            if (initial != null) saveRecordInternal(initial)
        }
        cleanupCandidates(listOfNotNull(oldPhoto))
        scheduler.reconcile()
    }

    suspend fun activateVehicle(id: String, pro: Boolean) {
        database.withTransaction {
            if (!pro) {
                dao.activeVehicles()
                    .filter {
                        it.id != id
                    }
                    .forEach {
                        dao.putVehicle(it.copy(archived = true))
                    }
            }
            dao.vehicle(id)?.let {
                dao.putVehicle(it.copy(archived = false))
            }
        }
        scheduler.reconcile()
    }

    suspend fun saveRecord(record: JournalRecord, attachments: List<Attachment> = emptyList()) {
        val oldPaths = dao.allAttachments().filter { it.recordId == record.id }.map { it.path }
        database.withTransaction {
            val vehicle = dao.vehicle(record.vehicleId) ?: throw DomainException("vehicle_missing")
            if (vehicle.archived) throw DomainException("archived_read_only")
            saveRecordInternal(record)
            dao.deleteRecordAttachments(record.id)
            attachments.forEach {
                dao.putAttachment(it)
            }
        }
        cleanupCandidates(oldPaths)
        scheduler.reconcile()
    }

    private suspend fun saveRecordInternal(record: JournalRecord) {
        val records = dao.vehicleRecords(record.vehicleId)
        val km = record.odometerKm?.decimal()
        if (km != null) {
            require(km >= BigDecimal.ZERO)
            val neighbors =
                records.filter {
                    it.id != record.id && it.odometerKm != null
                }
            val before =
                neighbors.filter {
                    it.timestamp < record.timestamp
                }.maxByOrNull {
                    it.timestamp
                }
            val after =
                neighbors.filter {
                    it.timestamp > record.timestamp
                }.minByOrNull {
                    it.timestamp
                }
            val same =
                neighbors.filter {
                    it.timestamp == record.timestamp
                }
            if (
                (before != null && km < before.odometerKm!!.decimal()) ||
                (after != null && km > after.odometerKm!!.decimal()) ||
                same.any {
                    km.compareTo(it.odometerKm!!.decimal()) != 0
                }
            ) {
                throw DomainException("odometer_conflict")
            }
        }
        val oldPlanId =
            records.find {
                it.id == record.id
            }?.planId
        dao.putRecord(record)
        (listOfNotNull(oldPlanId, record.planId).distinct()).forEach {
            recalculate(it, record.vehicleId)
        }
    }

    private suspend fun recalculate(planId: String, vehicleId: String) {
        dao.plan(planId)?.let { plan ->
            dao.putPlan(MaintenanceRules.recalculate(plan, dao.vehicleRecords(vehicleId)))
        }
    }

    suspend fun savePlan(plan: MaintenancePlan) {
        database.withTransaction {
            if (dao.vehicle(plan.vehicleId)?.archived != false) {
                throw DomainException("archived_read_only")
            }
            dao.putPlan(MaintenanceRules.recalculate(plan, dao.vehicleRecords(plan.vehicleId)))
        }
        scheduler.reconcile()
    }

    suspend fun deleteRecord(record: JournalRecord) {
        val candidates =
            dao.allAttachments().filter {
                it.recordId == record.id
            }.map {
                it.path
            }
        database.withTransaction {
            dao.deleteRecord(record.id)
            record.planId?.let {
                recalculate(it, record.vehicleId)
            }
        }
        cleanupCandidates(candidates)
        scheduler.reconcile()
    }

    suspend fun deleteVehicle(id: String) {
        val candidates =
            snapshot.first().let { state ->
                val recordIds =
                    state.records.filter {
                        it.vehicleId == id
                    }.map {
                        it.id
                    }.toSet()
                state.attachments.filter {
                    it.recordId in recordIds
                }.map {
                    it.path
                } +
                    listOfNotNull(
                        state.vehicles.find {
                            it.id == id
                        }?.photo
                    )
            }
        database.withTransaction {
            dao.deleteVehicle(id)
        }
        cleanupCandidates(candidates)
        scheduler.reconcile()
    }

    suspend fun deletePlan(id: String) {
        database.withTransaction {
            dao.deletePlan(id)
        }
        scheduler.reconcile()
    }

    suspend fun saveConversation(conversation: Conversation) = dao.putConversation(conversation)

    suspend fun deleteConversation(id: String) = dao.deleteConversation(id)

    private suspend fun cleanupCandidates(candidates: List<String>) {
        val references =
            dao.allAttachments().map {
                it.path
            }.toSet() +
                dao.allVehicles().mapNotNull {
                    it.photo
                }
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            candidates.filter {
                it !in references
            }.forEach {
                files.file(it).delete()
            }
        }
    }

    suspend fun clearAiHistory() = dao.clearConversations()

    suspend fun deleteAll() {
        database.withTransaction {
            dao.clearVehicles()
        }
        scheduler.reconcile()
    }
}
