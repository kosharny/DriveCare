package com.example.drivecare.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drivecare.R
import com.example.drivecare.core.model.*
import com.example.drivecare.data.billing.BillingRepository
import com.example.drivecare.data.billing.BillingState
import com.example.drivecare.data.local.PreferencesRepository
import com.example.drivecare.data.repository.DomainException
import com.example.drivecare.data.repository.DriveCareRepository
import com.example.drivecare.domain.*
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class FeatureUiState(
    val snapshot: StoreSnapshot = StoreSnapshot(),
    val prefs: AppPreferences = AppPreferences(),
    val billing: BillingState = BillingState(),
    val error: Int? = null
) {
    val vehicle: Vehicle?
        get() =
            snapshot.vehicles.find {
                it.id == prefs.selectedVehicleId
            }
                ?: snapshot.vehicles.firstOrNull {
                    !it.archived
                }
                ?: snapshot.vehicles.firstOrNull()

    val records: List<JournalRecord>
        get() =
            snapshot.records.filter {
                it.vehicleId == vehicle?.id
            }

    val plans: List<MaintenancePlan>
        get() =
            snapshot.plans.filter {
                it.vehicleId == vehicle?.id
            }

    val zone = ZoneId.of(prefs.reportingZone)
    val today = LocalDate.now(zone)
    val reading
        get() = latestReading(records)

    val mileageEstimate
        get() = FuelMileage.calculate(records)

    val statuses =
        plans.associate {
            it.id to MaintenanceRules.status(it, reading, today, zone)
        }
    val monthStatistics =
        vehicle?.let {

            StatisticsCalculator.calculate(
                records,
                it.fuelType,
                DateRange(
                    today.withDayOfMonth(1),
                    today.withDayOfMonth(1).plusMonths(1).minusDays(1)
                ),
                zone
            )
        }
    val loaded: Boolean
        get() = snapshot.loaded && prefs.loaded
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
open class FeatureViewModel(
    protected val repository: DriveCareRepository,
    protected val preferences: PreferencesRepository,
    protected val billing: BillingRepository
) : ViewModel() {
    private val error = MutableStateFlow<Int?>(null)
    private val minuteTicks =
        flow {

            while (currentCoroutineContext().isActive) {

                emit(System.currentTimeMillis() / 60_000)
                delay(60_000)
            }
        }
    private val reload = MutableStateFlow(0)
    val uiState =
        reload.flatMapLatest {
            combine(
                repository.snapshot,
                preferences.preferences,
                billing.state,
                error,
                minuteTicks,
                transform = ::createState
            ).catch {
                emit(FeatureUiState(error = R.string.operation_error))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeatureUiState())

    private fun createState(
        snapshot: StoreSnapshot,
        prefs: AppPreferences,
        purchase: BillingState,
        failure: Int?,
        tick: Long
    ): FeatureUiState = FeatureUiState(snapshot, prefs, purchase, failure)

    fun retryLoading() {
        reload.update { it + 1 }
    }

    fun selectVehicle(id: String) = perform {
        preferences.setString("vehicle", id)
    }

    fun acknowledge() {
        error.value = null
    }

    protected fun perform(operation: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                operation()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (failure: Exception) {
                error.value = errorResource(failure)
            }
        }
    }
}

fun errorResource(failure: Exception): Int = when ((failure as? DomainException)?.reason) {
    "odometer_conflict" -> R.string.odometer_conflict
    "pro_required" -> R.string.pro_required
    "archived_read_only" -> R.string.archived_read_only
    "vehicle_missing" -> R.string.vehicle_missing
    else -> R.string.operation_error
}
