package com.example.drivecare.feature.statistics

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.drivecare.core.ui.FeatureViewModel
import com.example.drivecare.data.billing.BillingRepository
import com.example.drivecare.data.local.PreferencesRepository
import com.example.drivecare.data.repository.DriveCareRepository
import com.example.drivecare.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.*

@HiltViewModel
class StatisticsViewModel
@Inject
constructor(
    repository: DriveCareRepository,
    preferences: PreferencesRepository,
    billing: BillingRepository,
    private val saved: SavedStateHandle,
    private val portability: PortabilityRepository
) : FeatureViewModel(repository, preferences, billing) {

    val reportCurrency = saved.getStateFlow("reportCurrency", "")
    val period = saved.getStateFlow("period", "month")
    val start = saved.getStateFlow("start", LocalDate.now().withDayOfMonth(1).toString())
    val end = saved.getStateFlow("end", LocalDate.now().toString())

    fun filter(key: String, value: String) {
        saved[key] = value
    }

    fun exportPdf(uri: Uri) = perform {
        val state = uiState.value
        val range = periodRange(period.value, start.value, end.value, state.today) ?: return@perform
        val vehicle = state.vehicle ?: return@perform
        portability.pdf(uri, vehicle, range, reportCurrency.value.ifBlank { vehicle.currency })
    }

    val result =
        combine(uiState, period, start, end) { state, selected, from, until ->
            val zone = ZoneId.of(state.prefs.reportingZone)
            val range =
                periodRange(selected, from, until, LocalDate.now(zone)) ?: return@combine null
            state.vehicle?.let {

                StatisticsCalculator.calculate(state.records, it.fuelType, range, zone)
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val advanced =
        combine(uiState, result, period, start, end) { state, stats, selected, from, until ->
            val range = periodRange(selected, from, until, state.today)
            val vehicle = state.vehicle
            if (stats == null || vehicle == null || range == null || !state.billing.pro) {
                null
            } else {
                AdvancedStatisticsCalculator.calculate(
                    state.snapshot,
                    vehicle,
                    stats,
                    range,
                    state.zone
                )
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

fun periodRange(period: String, from: String, until: String, today: LocalDate): DateRange? = try {
    when (period) {
        "month" ->
            DateRange(
                today.withDayOfMonth(1),
                today.withDayOfMonth(1).plusMonths(1).minusDays(1)
            )

        "previous" ->
            DateRange(
                today.withDayOfMonth(1).minusMonths(1),
                today.withDayOfMonth(1).minusDays(1)
            )

        "year" ->
            DateRange(today.withDayOfYear(1), today.withDayOfYear(1).plusYears(1).minusDays(1))

        "custom" -> DateRange(LocalDate.parse(from), LocalDate.parse(until))

        else -> DateRange(null, null)
    }
} catch (_: Exception) {
    null
}
