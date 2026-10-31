package com.example.drivecare.feature.settings

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.drivecare.R
import com.example.drivecare.core.ui.FeatureViewModel
import com.example.drivecare.data.billing.BillingRepository
import com.example.drivecare.data.local.AttachmentStore
import com.example.drivecare.data.local.PreferencesRepository
import com.example.drivecare.data.repository.DriveCareRepository
import com.example.drivecare.domain.PortabilityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsOperation(
    val busy: Boolean = false,
    val message: Int? = null,
    val restored: Boolean = false
)

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    repository: DriveCareRepository,
    preferences: PreferencesRepository,
    billing: BillingRepository,
    private val portability: PortabilityRepository,
    private val attachments: AttachmentStore
) : FeatureViewModel(repository, preferences, billing) {

    private val mutableOperation = MutableStateFlow(SettingsOperation())
    val operation = mutableOperation.asStateFlow()

    fun setting(key: String, value: String) = perform {
        when (key) {
            "currency" -> java.util.Currency.getInstance(value.uppercase())
            "zone" -> ZoneId.of(value)
        }
        preferences.setString(key, if (key == "currency") value.uppercase() else value)
    }

    fun revokeConsent() = perform {
        preferences.setBoolean("consent", false)
    }

    fun clearAi() = perform {
        repository.clearAiHistory()
    }

    fun deleteAll() = runOperation {
        repository.deleteAll()
        preferences.setString("vehicle", "")
        attachments.cleanup(emptySet(), immediately = true)
    }

    fun export(uri: Uri, type: String) = runOperation {
        if (type == "csv") portability.csv(uri) else portability.backup(uri)
    }

    fun restore(uri: Uri) = runOperation(R.string.invalid_backup, restored = true) {
        portability.restore(uri)
    }

    fun acknowledgeOperation() {
        mutableOperation.update {
            it.copy(message = null)
        }
    }

    private fun runOperation(
        error: Int = R.string.operation_error,
        restored: Boolean = false,
        operation: suspend () -> Unit
    ) {
        if (mutableOperation.value.busy) return
        mutableOperation.value = SettingsOperation(busy = true)
        viewModelScope.launch {
            try {
                operation()
                mutableOperation.value =
                    SettingsOperation(message = R.string.operation_success, restored = restored)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                mutableOperation.value = SettingsOperation(message = error)
            }
        }
    }
}
