package com.example.drivecare.feature.garage

import com.example.drivecare.core.model.Vehicle
import com.example.drivecare.core.ui.FeatureViewModel
import com.example.drivecare.data.billing.BillingRepository
import com.example.drivecare.data.local.PreferencesRepository
import com.example.drivecare.data.repository.DriveCareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GarageViewModel
@Inject
constructor(
    repository: DriveCareRepository,
    preferences: PreferencesRepository,
    billing: BillingRepository
) : FeatureViewModel(repository, preferences, billing) {

    fun archive(vehicle: Vehicle) = perform {
        repository.saveVehicle(vehicle.copy(archived = true), null, billing.state.value.pro)
    }

    fun activate(id: String) = perform {
        repository.activateVehicle(id, billing.state.value.pro)
        preferences.setString("vehicle", id)
    }

    fun delete(id: String) = perform {
        repository.deleteVehicle(id)
    }
}
