package com.example.drivecare.feature.maintenance

import androidx.lifecycle.SavedStateHandle
import com.example.drivecare.core.model.MaintenancePlan
import com.example.drivecare.core.ui.FeatureViewModel
import com.example.drivecare.data.billing.BillingRepository
import com.example.drivecare.data.local.PreferencesRepository
import com.example.drivecare.data.repository.DriveCareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class MaintenanceViewModel
@Inject
constructor(
    repository: DriveCareRepository,
    preferences: PreferencesRepository,
    billing: BillingRepository,
    private val saved: SavedStateHandle
) : FeatureViewModel(repository, preferences, billing) {

    val selectedDate = saved.getStateFlow("date", LocalDate.now().toString())
    val month = saved.getStateFlow("month", YearMonth.now().toString())
    val agenda = saved.getStateFlow("agenda", false)

    fun selectDate(value: String) {
        saved["date"] = value
    }

    fun moveMonth(offset: Long) {
        saved["month"] = YearMonth.parse(month.value).plusMonths(offset).toString()
    }

    fun today() {
        saved["month"] = YearMonth.now().toString()
        saved["date"] = LocalDate.now().toString()
    }

    fun showAgenda(value: Boolean) {
        saved["agenda"] = value
    }

    fun togglePause(plan: MaintenancePlan) = perform {
        repository.savePlan(plan.copy(paused = !plan.paused))
    }

    fun delete(plan: MaintenancePlan) = perform {
        repository.deletePlan(plan.id)
    }
}
