package com.example.drivecare.feature.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.drivecare.core.model.JournalRecord
import com.example.drivecare.core.ui.FeatureViewModel
import com.example.drivecare.data.billing.BillingRepository
import com.example.drivecare.data.local.PreferencesRepository
import com.example.drivecare.data.repository.DriveCareRepository
import com.example.drivecare.domain.DateRange
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.*

@HiltViewModel
class JournalViewModel
@Inject
constructor(
    repository: DriveCareRepository,
    preferences: PreferencesRepository,
    billing: BillingRepository,
    private val saved: SavedStateHandle
) : FeatureViewModel(repository, preferences, billing) {

    val query = saved.getStateFlow("query", "")
    val kind = saved.getStateFlow("kind", "all")
    val start = saved.getStateFlow("start", "")
    val end = saved.getStateFlow("end", "")
    val filtered =
        combine(uiState, query, kind, start, end) { state, text, type, from, until ->
            try {

                val range =
                    DateRange(
                        from.takeIf {
                            it.isNotBlank()
                        }?.let(LocalDate::parse),
                        until.takeIf {
                            it.isNotBlank()
                        }?.let(LocalDate::parse)
                    )
                state.records.filter {

                    (type == "all" || it.kind.name == type) &&
                        range.contains(it, state.zone) &&
                        (
                            text.isBlank() ||
                                listOf(it.title, it.notes, it.stationOrWorkshop).any { value ->
                                    value.contains(text, ignoreCase = true)
                                }
                            )
                }
            } catch (_: Exception) {

                null
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun filter(key: String, value: String) {
        saved[key] = value
    }

    fun delete(record: JournalRecord) = perform {
        repository.deleteRecord(record)
    }
}
