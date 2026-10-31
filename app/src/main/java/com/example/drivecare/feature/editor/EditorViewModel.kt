package com.example.drivecare.feature.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drivecare.R
import com.example.drivecare.core.ui.errorResource
import com.example.drivecare.data.catalogue.VehicleCatalogue
import com.example.drivecare.data.catalogue.VehicleSuggestions
import com.example.drivecare.data.local.AttachmentStore
import com.example.drivecare.data.repository.DomainException
import com.example.drivecare.domain.FuelMileage
import com.example.drivecare.domain.Units
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.RoundingMode
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val values: Map<String, String> = emptyMap(),
    val loaded: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: Int? = null,
    val isEdit: Boolean = false,
    val makes: List<String> = VehicleSuggestions.makes,
    val models: List<String> = emptyList(),
    val catalogueLoading: Boolean = false,
    val catalogueOnline: Boolean = false
) {
    val estimatedFuelDistanceKm: String?
        get() =
            try {
                if (!values["estimateMileage"].toBoolean()) {
                    null
                } else {
                    val volume =
                        values["canonical.volume"]?.toBigDecimal()
                            ?: Units.toLiters(
                                parseDecimal(values["volume"].orEmpty()),
                                values["volumeUnit"].orEmpty()
                            )
                    FuelMileage.distanceKm(
                        volume,
                        parseDecimal(values["estimatedConsumption"].orEmpty())
                    )
                        .stripTrailingZeros().toPlainString()
                }
            } catch (_: IllegalArgumentException) {
                null
            }
}

open class FormViewModel(
    private val savedState: SavedStateHandle,
    private val editor: EditorRepository,
    private val attachments: AttachmentStore,
    private val catalogue: VehicleCatalogue
) : ViewModel() {

    private val mutableState = MutableStateFlow(EditorUiState())
    val uiState = mutableState.asStateFlow()
    private var mode = ""
    private var id = ""
    private var vehicleId = ""
    private var planId = ""
    private var catalogueJob: Job? = null

    private fun refreshCatalogue(make: String) {
        catalogueJob?.cancel()
        mutableState.update {
            it.copy(catalogueLoading = true, models = VehicleSuggestions.models(make))
        }
        catalogueJob =
            viewModelScope.launch {
                try {
                    val makes = catalogue.makes()
                    mutableState.update { it.copy(makes = makes.names) }
                    val models = catalogue.models(make)
                    mutableState.update {
                        it.copy(
                            models = models.names,
                            catalogueLoading = false,
                            catalogueOnline = makes.online && (make.isBlank() || models.online)
                        )
                    }
                } catch (
                    cancel: CancellationException
                ) {
                    throw cancel
                } catch (
                    _: Exception
                ) {
                    mutableState.update {
                        it.copy(catalogueLoading = false, catalogueOnline = false)
                    }
                }
            }
    }

    fun initialize(
        mode: String,
        id: String,
        vehicleId: String,
        planId: String,
        draftTitle: String,
        draftNotes: String
    ) {
        if (this.mode.isNotEmpty()) return
        this.mode = mode
        this.id = id
        this.vehicleId = vehicleId
        this.planId = planId
        viewModelScope.launch {
            try {
                val restored = savedState.get<HashMap<String, String>>("draft")
                val values =
                    editor.initial(mode, id, vehicleId, planId).toMutableMap().apply {
                        if (restored != null) putAll(restored)
                        if (draftTitle.isNotBlank() && restored == null) put("title", draftTitle)
                        if (draftNotes.isNotBlank() && restored == null) put("notes", draftNotes)
                        if (restored != null && !restored.containsKey("dueMode")) {
                            put(
                                "dueMode",
                                when {
                                    !get(
                                        "dueDate"
                                    ).isNullOrBlank() && !get("dueKm").isNullOrBlank() -> "both"

                                    !get("dueKm").isNullOrBlank() -> "distance"

                                    else -> "date"
                                }
                            )
                        }
                        if (restored != null && !restored.containsKey("repeatPlan")) {
                            val hasInterval =
                                !get("intervalMonths").isNullOrBlank() ||
                                    !get("intervalKm").isNullOrBlank()
                            put("repeatPlan", hasInterval.toString())
                        }
                    }
                mutableState.value =
                    EditorUiState(
                        values = values,
                        loaded = true,
                        isEdit = id.isNotBlank(),
                        models = VehicleSuggestions.models(values["make"].orEmpty())
                    )
                if (mode == "vehicle") refreshCatalogue(values["make"].orEmpty())
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(error = R.string.operation_error)
                }
            }
        }
    }

    fun change(key: String, value: String) {
        if (uiState.value.saving) return
        val fields = uiState.value.values.toMutableMap()
        if (key == "recordMileage" && value.toBoolean()) fields["estimateMileage"] = "false"
        if (key == "estimateMileage" && value.toBoolean()) fields["recordMileage"] = "false"
        val previousUnit = fields[key].orEmpty()
        if (key == "make" &&
            VehicleSuggestions.normalized(previousUnit) != VehicleSuggestions.normalized(value)
        ) {
            fields["model"] = ""
        }
        if (key == "dueAfter") {
            fields["odometerBaseline"]?.toBigDecimalOrNull()?.let { baseline ->
                fields["dueKm"] = (baseline + parseDecimal(value)).toPlainString()
                fields.remove("canonical.dueKm")
            }
        }
        if (key in
            setOf("odometer", "purchaseMileage", "dueKm", "intervalKm", "warningKm", "volume")
        ) {
            fields.remove("canonical.$key")
        }
        if (key == "distanceUnit" && previousUnit.isNotBlank() && previousUnit != value) {
            listOf(
                "odometer",
                "purchaseMileage",
                "dueKm",
                "intervalKm",
                "warningKm",
                "odometerBaseline"
            ).forEach { field ->
                fields[field]
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let { raw ->
                        try {
                            val canonical =
                                fields["canonical.$field"]?.toBigDecimal()
                                    ?: com.example.drivecare.domain.Units.toKm(
                                        parseDecimal(raw),
                                        previousUnit
                                    )
                            fields["canonical.$field"] = canonical.toPlainString()
                            fields[field] =
                                com.example.drivecare.domain.Units.fromKm(canonical, value)
                                    .stripTrailingZeros()
                                    .toPlainString()
                        } catch (_: IllegalArgumentException) {
                            // Keep unfinished numeric input available for correction.
                        }
                    }
            }
        }
        if (key == "volumeUnit" && previousUnit.isNotBlank() && previousUnit != value) {
            fields["volume"]
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let { raw ->
                    try {
                        val canonical =
                            fields["canonical.volume"]?.toBigDecimal()
                                ?: com.example.drivecare.domain.Units.toLiters(
                                    parseDecimal(raw),
                                    previousUnit
                                )
                        fields["canonical.volume"] = canonical.toPlainString()
                        fields["volume"] =
                            com.example.drivecare.domain.Units.fromLiters(canonical, value)
                                .stripTrailingZeros()
                                .toPlainString()
                    } catch (_: IllegalArgumentException) {
                        // Keep unfinished numeric input available for correction.
                    }
                }
        }
        fields[key] = value
        if (mode == "fuel") {
            try {
                val volume = parseDecimal(fields["volume"].orEmpty())
                if (volume.signum() > 0) {
                    if (key == "unitPrice") {
                        fields["amount"] =
                            volume
                                .multiply(parseDecimal(value))
                                .setScale(
                                    com.example.drivecare.domain.Money.scale(
                                        fields["currency"].orEmpty()
                                    ),
                                    RoundingMode.HALF_UP
                                )
                                .toPlainString()
                    } else if (key in listOf("volume", "amount", "currency", "volumeUnit")) {
                        fields["unitPrice"] =
                            parseDecimal(fields["amount"].orEmpty())
                                .divide(volume, 6, RoundingMode.HALF_UP)
                                .stripTrailingZeros()
                                .toPlainString()
                    }
                }
            } catch (_: IllegalArgumentException) {
                // Recalculate once both numeric inputs are valid.
            }
        }
        savedState["draft"] = HashMap(fields)
        mutableState.update {
            it.copy(values = fields, error = null)
        }
        if (key == "make" && mode == "vehicle") refreshCatalogue(value)
    }

    fun attach(uri: Uri, photo: Boolean) {
        if (uiState.value.saving) return
        mutableState.update {
            it.copy(saving = true)
        }
        viewModelScope.launch {
            try {
                val (path, mime) = attachments.copy(uri, photo)
                mutableState.update {
                    it.copy(saving = false)
                }
                if (photo) {
                    change("photo", path)
                } else {
                    val current = uiState.value.values["attachments"].orEmpty()
                    change(
                        "attachments",
                        listOf(current, "$path;$mime").filter {
                            it.isNotBlank()
                        }.joinToString("|")
                    )
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(saving = false, error = R.string.operation_error)
                }
            }
        }
    }

    fun save() {
        if (uiState.value.saving || !uiState.value.loaded) return
        mutableState.update {
            it.copy(saving = true, error = null)
        }
        viewModelScope.launch {
            try {
                editor.save(mode, id, vehicleId, planId, uiState.value.values)
                mutableState.update {
                    it.copy(saving = false, saved = true)
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (failure: Exception) {
                mutableState.update {
                    it.copy(
                        saving = false,
                        error =
                            if (failure is DomainException) {
                                errorResource(failure)
                            } else {
                                R.string.invalid_fields
                            }
                    )
                }
            }
        }
    }
}

@HiltViewModel
class VehicleEditorViewModel
@Inject
constructor(
    saved: SavedStateHandle,
    editor: EditorRepository,
    attachments: AttachmentStore,
    catalogue: VehicleCatalogue
) : FormViewModel(saved, editor, attachments, catalogue)

@HiltViewModel
class JournalEditorViewModel
@Inject
constructor(
    saved: SavedStateHandle,
    editor: EditorRepository,
    attachments: AttachmentStore,
    catalogue: VehicleCatalogue
) : FormViewModel(saved, editor, attachments, catalogue)

@HiltViewModel
class PlanEditorViewModel
@Inject
constructor(
    saved: SavedStateHandle,
    editor: EditorRepository,
    attachments: AttachmentStore,
    catalogue: VehicleCatalogue
) : FormViewModel(saved, editor, attachments, catalogue)
