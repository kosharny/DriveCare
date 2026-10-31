package com.example.drivecare.feature.garage

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drivecare.R
import com.example.drivecare.core.ui.*

@Composable
fun GarageRoute(
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onSelected: () -> Unit,
    onPro: () -> Unit
) {
    val vm: GarageViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    var deletion by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    PageColumn(Artwork.GARAGE) {
        Text(stringResource(R.string.garage), style = MaterialTheme.typography.headlineMedium)
        if (!state.billing.pro && state.snapshot.vehicles.count {
                !it.archived
            } > 1
        ) {
            Text(
                stringResource(R.string.choose_free_vehicle),
                color = MaterialTheme.colorScheme.error
            )
        }
        Button(
            onClick = {
                if (!state.billing.pro &&
                    state.snapshot.vehicles.any {
                        !it.archived
                    }
                ) {
                    onPro()
                } else {
                    onAdd()
                }
            }
        ) {
            Text(stringResource(R.string.add_vehicle))
        }
        state.snapshot.vehicles.forEach { vehicle ->
            InfoCard(vehicle.displayName, "${vehicle.make} ${vehicle.model} · ${vehicle.year}") {
                vehicle.photo?.let { VehiclePhoto(it, vehicle.displayName) }
                if (vehicle.archived) Text(stringResource(R.string.archived))
                OutlinedButton(
                    onClick = {
                        vm.selectVehicle(vehicle.id)
                        onSelected()
                    }
                ) {
                    Text(stringResource(R.string.select_vehicle))
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = {
                        onEdit(vehicle.id)
                    }) {
                        Text(stringResource(R.string.edit))
                    }
                    TextButton(
                        onClick = {
                            if (vehicle.archived || !state.billing.pro) {
                                vm.activate(vehicle.id)
                            } else {
                                vm.archive(vehicle)
                            }
                        }
                    ) {
                        Text(
                            stringResource(
                                if (vehicle.archived || !state.billing.pro) {
                                    R.string.activate
                                } else {
                                    R.string.archive
                                }
                            )
                        )
                    }
                    TextButton(onClick = {
                        deletion = vehicle.id
                    }) {
                        Text(stringResource(R.string.delete))
                    }
                }
                if (!vehicle.archived && !state.billing.pro) {
                    TextButton(onClick = {
                        vm.archive(vehicle)
                    }) {
                        Text(stringResource(R.string.archive))
                    }
                }
            }
        }
        if (state.snapshot.vehicles.isEmpty()) Text(stringResource(R.string.no_vehicle_help))
    }
    Message(state.error, vm::acknowledge)
    deletion?.let { id ->
        DeleteConfirmation(
            onDismiss = {
                deletion = null
            },
            onConfirm = {
                deletion = null
                vm.delete(id)
            }
        )
    }
}
