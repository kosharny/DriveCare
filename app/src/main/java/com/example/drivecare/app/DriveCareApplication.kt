package com.example.drivecare.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.drivecare.core.notifications.ReminderWorker
import com.example.drivecare.data.local.PreferencesRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class DriveCareApplication : Application() {

    @Inject lateinit var preferences: PreferencesRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            preferences.initialize()
        }
        if (FirebaseApp.initializeApp(this) != null) {
            AppAttestation.install()
            FirebaseRemoteConfig.getInstance()
                .setDefaultsAsync(
                    mapOf(
                        "ai_enabled" to true,
                        "ai_model" to "gemini-3.5-flash-lite",
                        "ai_daily_allowance" to 5L
                    )
                )
        }
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "maintenance-reconciliation",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ReminderWorker>(6, TimeUnit.HOURS).build()
            )
    }
}
