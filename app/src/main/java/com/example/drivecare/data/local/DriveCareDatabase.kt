package com.example.drivecare.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.drivecare.core.model.Attachment
import com.example.drivecare.core.model.Conversation
import com.example.drivecare.core.model.JournalRecord
import com.example.drivecare.core.model.MaintenancePlan
import com.example.drivecare.core.model.Vehicle
import kotlinx.coroutines.flow.Flow

@Dao
interface DriveCareDao {

    @Query("SELECT * FROM vehicles ORDER BY archived, make, model")
    fun vehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM records ORDER BY timestamp DESC, id")
    fun records(): Flow<List<JournalRecord>>

    @Query("SELECT * FROM plans ORDER BY dueDate, title")
    fun plans(): Flow<List<MaintenancePlan>>

    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    fun conversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM attachments")
    fun attachments(): Flow<List<Attachment>>

    @Query("SELECT * FROM vehicles")
    suspend fun allVehicles(): List<Vehicle>

    @Query("SELECT * FROM records")
    suspend fun allRecords(): List<JournalRecord>

    @Query("SELECT * FROM plans")
    suspend fun allPlans(): List<MaintenancePlan>

    @Query("SELECT * FROM conversations")
    suspend fun allConversations(): List<Conversation>

    @Query("SELECT * FROM attachments")
    suspend fun allAttachments(): List<Attachment>

    @Query("SELECT * FROM records WHERE vehicleId = :vehicleId ORDER BY timestamp, id")
    suspend fun vehicleRecords(vehicleId: String): List<JournalRecord>

    @Query("SELECT * FROM plans WHERE id = :id")
    suspend fun plan(id: String): MaintenancePlan?

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun vehicle(id: String): Vehicle?

    @Query("SELECT * FROM vehicles WHERE archived = 0")
    suspend fun activeVehicles(): List<Vehicle>

    @Upsert suspend fun putVehicle(vehicle: Vehicle)

    @Upsert suspend fun putRecord(record: JournalRecord)

    @Upsert suspend fun putPlan(plan: MaintenancePlan)

    @Upsert suspend fun putConversation(conversation: Conversation)

    @Upsert suspend fun putAttachment(attachment: Attachment)

    @Query("DELETE FROM vehicles WHERE id = :id")
    suspend fun deleteVehicle(id: String)

    @Query("DELETE FROM attachments WHERE recordId = :recordId")
    suspend fun deleteRecordAttachments(recordId: String)

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteRecord(id: String)

    @Query("DELETE FROM plans WHERE id = :id")
    suspend fun deletePlan(id: String)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("DELETE FROM conversations")
    suspend fun clearConversations()

    @Query("DELETE FROM vehicles")
    suspend fun clearVehicles()
}

@Database(
    entities =
        [
            Vehicle::class,
            JournalRecord::class,
            MaintenancePlan::class,
            Attachment::class,
            Conversation::class
        ],
    version = 2,
    exportSchema = true
)
abstract class DriveCareDatabase : RoomDatabase() {

    abstract fun dao(): DriveCareDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE records ADD COLUMN estimatedConsumptionLPer100Km TEXT")
                    db.execSQL("ALTER TABLE records ADD COLUMN estimateAnchorRecordId TEXT")
                }
            }
    }
}
