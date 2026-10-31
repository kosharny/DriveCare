package com.example.drivecare.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.drivecare.core.model.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore("drivecare_settings")

@Singleton
class PreferencesRepository @Inject constructor(@ApplicationContext context: Context) {

    private val store = context.settingsStore
    val preferences =
        store.data.map { value ->
            AppPreferences(
                loaded = true,
                onboardingDone = value[booleanPreferencesKey("onboarding")] ?: false,
                selectedVehicleId = value[stringPreferencesKey("vehicle")] ?: "",
                distanceUnit = value[stringPreferencesKey("distance")] ?: "km",
                volumeUnit = value[stringPreferencesKey("volume")] ?: "L",
                currency = value[stringPreferencesKey("currency")] ?: "UAH",
                theme = value[stringPreferencesKey("theme")] ?: "system",
                reportingZone = value[stringPreferencesKey("zone")] ?: ZoneId.systemDefault().id,
                aiConsent = value[booleanPreferencesKey("consent")] ?: false,
                aiDay = value[stringPreferencesKey("ai_day")] ?: "",
                aiCount = value[intPreferencesKey("ai_count")] ?: 0,
                aiLastSuccess = value[longPreferencesKey("ai_last")] ?: 0
            )
        }

    suspend fun initialize() {
        store.edit { value ->
            if (value[stringPreferencesKey("zone")] == null) {
                value[stringPreferencesKey("zone")] = ZoneId.systemDefault().id
            }
        }
    }

    suspend fun setString(key: String, value: String) {
        store.edit {
            it[stringPreferencesKey(key)] = value
        }
    }

    suspend fun setBoolean(key: String, value: Boolean) {
        store.edit {
            it[booleanPreferencesKey(key)] = value
        }
    }

    suspend fun successfulAiResponse(now: Long) {
        val zone = ZoneId.of(preferences.first().reportingZone)
        val today = LocalDate.now(zone).toString()
        store.edit { value ->
            val count =
                if (value[stringPreferencesKey("ai_day")] == today) {
                    value[intPreferencesKey("ai_count")] ?: 0
                } else {
                    0
                }
            value[stringPreferencesKey("ai_day")] = today
            value[intPreferencesKey("ai_count")] = count + 1
            value[longPreferencesKey("ai_last")] = now
        }
    }
}
