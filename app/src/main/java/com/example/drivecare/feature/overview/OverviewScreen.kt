package com.example.drivecare.feature.overview

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drivecare.R
import com.example.drivecare.core.model.date
import com.example.drivecare.core.ui.*
import com.example.drivecare.domain.DueStatus
import com.example.drivecare.feature.maintenance.PlanCard

@Composable
fun OverviewRoute(
    onAdd: () -> Unit,
    onAction: (String) -> Unit,
    onPlan: (String) -> Unit,
    onAi: () -> Unit
) {
    val vm: OverviewViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    if (!state.loaded) {
        BrandLoadingScreen(error = state.error, onRetry = vm::retryLoading)
        return
    }
    OverviewScreen(state, onAdd, onAction, onPlan, onAi)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverviewScreen(
    state: FeatureUiState,
    onAdd: () -> Unit,
    onAction: (String) -> Unit,
    onPlan: (String) -> Unit,
    onAi: () -> Unit
) {
    val vehicle = state.vehicle
    if (vehicle == null) {
        EmptyVehicle(onAdd, Artwork.OVERVIEW)
        return
    }
    val locale = appLocale()
    val stats = state.monthStatistics
    val estimate = state.mileageEstimate
    PageColumn(Artwork.OVERVIEW) {
        InfoCard(vehicle.displayName, "${vehicle.make} ${vehicle.model} · ${vehicle.year}") {
            vehicle.photo?.let { photo ->
                VehiclePhoto(photo, vehicle.displayName)
            }
            Text(stringResource(fuelLabel(vehicle.fuelType)))
            Text(
                stringResource(
                    if (estimate ==
                        null
                    ) {
                        R.string.last_mileage
                    } else {
                        R.string.estimated_mileage_title
                    }
                ),
                style = MaterialTheme.typography.labelMedium
            )
            state.reading?.let { reading ->
                Text(
                    if (estimate == null) {
                        distance(reading.odometerKm!!, state.prefs.distanceUnit)
                    } else {
                        stringResource(
                            R.string.approximate_value,
                            distance(estimate.odometerKm.toPlainString(), state.prefs.distanceUnit)
                        )
                    },
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    if (estimate == null) {
                        date(reading.date(state.zone).toString(), locale)
                    } else {
                        stringResource(
                            R.string.actual_mileage_reference,
                            distance(reading.odometerKm!!, state.prefs.distanceUnit),
                            date(reading.date(state.zone).toString(), locale)
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (estimate != null) {
                Text(
                    stringResource(R.string.estimated_mileage_warning),
                    style = MaterialTheme.typography.bodySmall
                )
                if (!estimate.complete) {
                    Text(
                        stringResource(R.string.estimate_incomplete),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (vehicle.archived) {
                Text(stringResource(R.string.archived), color = MaterialTheme.colorScheme.error)
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "fuel" to R.string.add_fuel,
                "expense" to R.string.add_expense,
                "service" to R.string.log_service,
                "odometer" to R.string.update_odometer
            )
                .forEach { (mode, label) ->
                    FilledTonalButton(onClick = {
                        onAction(mode)
                    }, enabled = !vehicle.archived) {
                        Text(stringResource(label))
                    }
                }
        }
        BoxWithConstraints {
            val wide = maxWidth >= 700.dp
            FlowRow(
                maxItemsInEachRow = if (wide) 2 else 1,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    Modifier
                        .weight(1f)
                ) {
                    InfoCard(
                        stringResource(R.string.fuel_spending),
                        stringResource(R.string.this_month)
                    ) {
                        if (stats?.totals.isNullOrEmpty()) Text(stringResource(R.string.no_data))
                        stats?.totals?.forEach {
                            Text(
                                money(it.fuelMinor, it.currency, locale),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }
                Column(
                    Modifier
                        .weight(1f)
                ) {
                    InfoCard(
                        stringResource(R.string.ownership_total),
                        stringResource(R.string.this_month)
                    ) {
                        if (stats?.totals.isNullOrEmpty()) Text(stringResource(R.string.no_data))
                        stats?.totals?.forEach {
                            Text(
                                money(it.totalMinor, it.currency, locale),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }
            }
        }
        InfoCard(stringResource(R.string.fuel_consumption)) {
            val consumption = stats?.consumption?.litersPer100Km
            Text(
                if (consumption != null) {
                    com.example.drivecare.core.ui.consumption(
                        consumption,
                        state.prefs.distanceUnit,
                        state.prefs.volumeUnit
                    )
                } else {
                    stringResource(R.string.not_enough_data)
                },
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                stringResource(
                    if (stats?.consumption?.unsupportedDualFuel == true) {
                        R.string.dual_fuel_help
                    } else {
                        R.string.consumption_method
                    }
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(stringResource(R.string.next_maintenance), style = MaterialTheme.typography.titleLarge)
        val overdue =
            state.statuses.values.count {
                it.status == DueStatus.OVERDUE
            }
        if (overdue > 0) {
            Text(
                stringResource(R.string.overdue) + ": $overdue",
                color = MaterialTheme.colorScheme.error
            )
        }
        if (state.plans.isEmpty()) Text(stringResource(R.string.empty_plans))
        state.plans
            .filter {
                !it.completed && !it.paused
            }
            .take(4)
            .forEach { plan ->
                PlanCard(
                    plan,
                    state.statuses.getValue(plan.id),
                    state.prefs.distanceUnit,
                    onClick = {
                        onPlan(plan.id)
                    }
                )
            }
        InfoCard(stringResource(R.string.ai_garage), stringResource(R.string.ai_grounding)) {
            OutlinedButton(onClick = onAi) {
                Text(stringResource(R.string.weak_points))
            }
        }
    }
}
