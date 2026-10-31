package com.example.drivecare.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.drivecare.MainActivity
import com.example.drivecare.R
import com.example.drivecare.data.local.DriveCareDatabase
import com.example.drivecare.data.local.PreferencesRepository
import com.example.drivecare.data.repository.DriveCareRepository
import com.example.drivecare.domain.DueStatus
import com.example.drivecare.domain.MaintenanceRules
import com.example.drivecare.domain.latestReading
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderDependencies {

    fun repository(): DriveCareRepository

    fun preferences(): PreferencesRepository

    fun database(): DriveCareDatabase
}

class ReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = ContextCompat.getContextForLanguage(applicationContext)
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return Result.success()
        val platform = context.getSystemService(NotificationManager::class.java)
        platform.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                context.getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
                .apply {
                    description = context.getString(R.string.notification_channel_help)
                }
        )
        if (
            platform.getNotificationChannel(CHANNEL)?.importance ==
            NotificationManager.IMPORTANCE_NONE
        ) {
            return Result.success()
        }
        val dependencies =
            EntryPointAccessors.fromApplication(context, ReminderDependencies::class.java)
        val snapshot = dependencies.repository().snapshot.first()
        val zone = ZoneId.of(dependencies.preferences().preferences.first().reportingZone)
        val today = LocalDate.now(zone)
        val eligible =
            snapshot.plans.filter { plan ->
                val vehicle =
                    snapshot.vehicles.find {
                        it.id == plan.vehicleId
                    }
                val reading =
                    latestReading(
                        snapshot.records.filter {
                            it.vehicleId == plan.vehicleId
                        }
                    )
                plan.reminders && !plan.paused && !plan.completed && vehicle?.archived == false &&
                    MaintenanceRules.status(plan, reading, today, zone).status in
                    listOf(DueStatus.DUE_SOON, DueStatus.DUE, DueStatus.OVERDUE)
            }.map {
                it.id
            }.toSet()
        platform.activeNotifications.filter {
            it.notification.channelId == CHANNEL && it.tag !in eligible
        }.forEach {
            manager.cancel(it.tag, it.id)
        }
        snapshot.plans
            .filter {
                it.reminders && !it.paused && !it.completed
            }
            .forEach { plan ->
                val vehicle =
                    snapshot.vehicles.find {
                        it.id == plan.vehicleId && !it.archived
                    }
                        ?: return@forEach
                val reading =
                    latestReading(
                        snapshot.records.filter {
                            it.vehicleId == plan.vehicleId
                        }
                    )
                val status = MaintenanceRules.status(plan, reading, today, zone).status
                if (status !in listOf(DueStatus.DUE_SOON, DueStatus.DUE, DueStatus.OVERDUE)) {
                    return@forEach
                }
                val database = dependencies.database()
                database.withTransaction {
                    val current = database.dao().plan(plan.id) ?: return@withTransaction
                    if (current.lastNotifiedDay == today.toString()) return@withTransaction
                    val intent =
                        Intent(context, MainActivity::class.java)
                            .putExtra("vehicleId", plan.vehicleId)
                            .putExtra("planId", plan.id)
                            .setAction("drivecare.plan.${plan.id}")
                    val pending =
                        PendingIntent.getActivity(
                            context,
                            plan.id.hashCode(),
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    val notification =
                        NotificationCompat.Builder(context, CHANNEL)
                            .setSmallIcon(R.drawable.ic_car)
                            .setContentTitle(context.getString(R.string.notification_title))
                            .setContentText(
                                context.getString(
                                    R.string.notification_body,
                                    vehicle.displayName,
                                    plan.title
                                )
                            )
                            .setContentIntent(pending)
                            .setAutoCancel(true)
                            .setOnlyAlertOnce(true)
                            .build()
                    manager.notify(plan.id, 0, notification)
                    database.dao().putPlan(current.copy(lastNotifiedDay = today.toString()))
                }
            }
        return Result.success()
    }

    companion object {

        const val CHANNEL = "maintenance"
    }
}
