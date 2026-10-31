package com.example.drivecare.feature.aigarage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.drivecare.R
import com.example.drivecare.core.model.VehicleAdvice
import com.example.drivecare.core.ui.FeatureViewModel
import com.example.drivecare.data.billing.BillingRepository
import com.example.drivecare.data.local.PreferencesRepository
import com.example.drivecare.data.remote.ai.AiFailure
import com.example.drivecare.data.remote.ai.AiRepository
import com.example.drivecare.data.repository.DriveCareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class AiUiState(
    val question: String = "",
    val mode: String = "weakPoints",
    val includeTotals: Boolean = false,
    val sending: Boolean = false,
    val consentRequired: Boolean = false,
    val message: Int? = null,
    val selectedId: String = "",
    val advice: VehicleAdvice? = null
)

@HiltViewModel
class AiGarageViewModel
@Inject
constructor(
    repository: DriveCareRepository,
    preferences: PreferencesRepository,
    billing: BillingRepository,
    private val ai: AiRepository,
    private val saved: SavedStateHandle
) : FeatureViewModel(repository, preferences, billing) {

    private val interaction =
        MutableStateFlow(
            AiUiState(
                question = saved["question"] ?: "",
                mode = saved["mode"] ?: "weakPoints",
                includeTotals = saved["include"] ?: false,
                selectedId = saved["selected"] ?: ""
            )
        )
    private var request: Job? = null
    val configured
        get() = ai.configured

    val aiState =
        combine(interaction, uiState) { action, feature ->
            val conversation =
                feature.snapshot.conversations.find {
                    it.id == action.selectedId && it.vehicleId == feature.vehicle?.id
                }
            val advice =
                conversation?.let {

                    try {

                        Json.decodeFromString<VehicleAdvice>(it.adviceJson)
                    } catch (_: Exception) {

                        null
                    }
                }
            action.copy(advice = advice)
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), interaction.value)

    fun question(value: String) {
        saved["question"] = value.take(3000)
        interaction.update {
            it.copy(question = value.take(3000))
        }
    }

    fun shortcut(mode: String, text: String) {
        saved["mode"] = mode
        interaction.update {
            it.copy(mode = mode)
        }
        question(text)
    }

    fun include(value: Boolean) {
        saved["include"] = value
        interaction.update {
            it.copy(includeTotals = value)
        }
    }

    fun select(id: String) {
        saved["selected"] = id
        interaction.update {
            it.copy(selectedId = id)
        }
    }

    fun dismissConsent() {
        interaction.update {
            it.copy(consentRequired = false)
        }
    }

    fun consentAndSend(language: String) {
        viewModelScope.launch {
            preferences.setBoolean("consent", true)
            interaction.update {
                it.copy(consentRequired = false)
            }
            send(language, consentJustGranted = true)
        }
    }

    fun send(language: String, consentJustGranted: Boolean = false) {
        if (request?.isActive == true || interaction.value.question.isBlank()) return
        if (!uiState.value.prefs.aiConsent && !consentJustGranted) {
            interaction.update {
                it.copy(consentRequired = true)
            }
            return
        }
        val vehicle = uiState.value.vehicle ?: return
        val action = interaction.value
        interaction.update {
            it.copy(sending = true, message = null)
        }
        request =
            viewModelScope.launch {
                try {
                    val conversation =
                        ai.ask(
                            vehicle,
                            action.question,
                            language,
                            action.mode,
                            action.includeTotals
                        )
                    select(conversation.id)
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (failure: AiFailure) {
                    interaction.update {
                        it.copy(message = failure.messageResource)
                    }
                } catch (_: Exception) {
                    interaction.update {
                        it.copy(message = R.string.ai_error)
                    }
                } finally {
                    interaction.update {
                        it.copy(sending = false)
                    }
                }
            }
    }

    fun cancel() {
        request?.cancel()
        interaction.update {
            it.copy(sending = false, message = R.string.ai_cancelled)
        }
    }

    fun delete(id: String) = perform {
        repository.deleteConversation(id)
        select("")
    }
}
