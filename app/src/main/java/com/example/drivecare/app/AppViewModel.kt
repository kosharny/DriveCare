package com.example.drivecare.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.drivecare.core.ui.FeatureViewModel
import com.example.drivecare.data.billing.BillingRepository
import com.example.drivecare.data.local.PreferencesRepository
import com.example.drivecare.data.repository.DriveCareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AppViewModel
@Inject
constructor(
    repository: DriveCareRepository,
    preferences: PreferencesRepository,
    billing: BillingRepository,
    private val savedState: SavedStateHandle
) : FeatureViewModel(repository, preferences, billing) {

    private val intro = MutableStateFlow(savedState.get<Boolean>("brandIntroPlayed") != true)
    val showingIntro = intro.asStateFlow()

    private var introStarted = false

    fun onBrandVisible() {
        if (intro.value && !introStarted) {
            introStarted = true
            viewModelScope.launch {
                delay(650)
                savedState["brandIntroPlayed"] = true
                intro.value = false
            }
        }
    }

    fun finishOnboarding() = perform {
        preferences.setBoolean("onboarding", true)
    }
}
