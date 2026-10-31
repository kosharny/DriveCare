package com.example.drivecare.feature.editor

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drivecare.R
import com.example.drivecare.core.model.Category
import com.example.drivecare.core.model.FuelType
import com.example.drivecare.core.ui.*

@Composable
fun EditorRoute(
    mode: String,
    id: String,
    vehicleId: String,
    planId: String,
    draftTitle: String,
    draftNotes: String,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val vm: FormViewModel =
        when (mode) {
            "vehicle" -> hiltViewModel<VehicleEditorViewModel>()
            "plan" -> hiltViewModel<PlanEditorViewModel>()
            else -> hiltViewModel<JournalEditorViewModel>()
        }
    val state by vm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(mode, id, vehicleId, planId) {
        vm.initialize(mode, id, vehicleId, planId, draftTitle, draftNotes)
    }
    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) vm.attach(uri, mode == "vehicle")
        }
    val context = LocalContext.current
    var denied by remember {
        mutableStateOf(false)
    }
    val notifications =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            denied = !granted
        }
    if (denied) {
        Message(R.string.notification_denied) {
            denied = false
        }
    }
    EditorScreen(
        mode,
        state,
        { key, value ->
            vm.change(key, value)
            if (
                key == "reminders" &&
                value == "true" &&
                !NotificationManagerCompat.from(context).areNotificationsEnabled()
            ) {
                if (Build.VERSION.SDK_INT >= 33) {
                    notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    denied = true
                }
            }
        },
        vm::save,
        onBack
    ) {
        picker.launch(
            if (mode == "vehicle") {
                arrayOf("image/jpeg", "image/png", "image/webp")
            } else {
                arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf")
            }
        )
    }
}

@Composable
fun EditorScreen(
    mode: String,
    state: EditorUiState,
    onChange: (String, String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onAttach: () -> Unit
) {
    var vehicleStep by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    val title =
        when (mode) {
            "vehicle" -> if (state.isEdit) R.string.edit_vehicle else R.string.add_vehicle
            "plan" -> R.string.add_plan
            "fuel" -> R.string.add_fuel
            "service" -> R.string.log_service
            "expense" -> R.string.add_expense
            "odometer" -> R.string.update_odometer
            else -> R.string.save_note
        }
    val artwork =
        when (mode) {
            "vehicle" -> Artwork.VEHICLE
            "plan" -> Artwork.PLAN
            "fuel" -> Artwork.FUEL
            "service" -> Artwork.SERVICE
            "expense" -> Artwork.EXPENSE
            "odometer" -> Artwork.ODOMETER
            else -> Artwork.NOTE
        }
    val category = state.values["category"]?.let { Category.valueOf(it) } ?: Category.ENGINE_OIL
    val categoryTitle = stringResource(categoryLabel(category))
    PageColumn(artwork) {
        Column(
            modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(title), style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = {
                if (mode == "vehicle" &&
                    vehicleStep > 0
                ) {
                    vehicleStep--
                } else {
                    onBack()
                }
            }) {
                Text(stringResource(R.string.back))
            }
            if (!state.loaded) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                when (mode) {
                    "vehicle" -> VehicleForm(state, vehicleStep, onChange, onAttach)
                    "plan" -> PlanForm(state, onChange)
                    "fuel" -> FuelForm(state, onChange, onAttach)
                    else -> JournalRecordForm(mode, state, onChange, onAttach)
                }
                if (state.values["fuelType"] ==
                    FuelType.PLUG_IN_HYBRID.name
                ) {
                    Text(
                        stringResource(R.string.phev_help),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                state.error?.let {
                    Text(stringResource(it), color = MaterialTheme.colorScheme.error)
                }
                val nextStep = mode == "vehicle" && vehicleStep == 0
                val identityReady =
                    listOf("make", "model", "year").all {
                        !state.values[it].isNullOrBlank()
                    }
                Button(
                    onClick = {
                        if (nextStep) {
                            vehicleStep = 1
                        } else {
                            if (mode in listOf("plan", "service") &&
                                state.values["title"].isNullOrBlank()
                            ) {
                                onChange("title", categoryTitle)
                            }
                            onSave()
                        }
                    },
                    enabled = !state.saving && (!nextStep || identityReady),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) {
                    Text(
                        stringResource(
                            if (state.saving) {
                                R.string.loading
                            } else if (nextStep) {
                                R.string.next
                            } else {
                                R.string.save
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
internal fun FormField(
    key: String,
    label: Int,
    state: EditorUiState,
    onChange: (String, String) -> Unit,
    numeric: Boolean = false,
    multiline: Boolean = false,
    hint: String = ""
) {
    OutlinedTextField(
        value = state.values[key].orEmpty(),
        onValueChange = {
            onChange(key, it)
        },
        label = {
            Text(stringResource(label))
        },
        placeholder =
            if (hint.isNotEmpty()) {
                {
                    Text(hint)
                }
            } else {
                null
            },
        singleLine = !multiline,
        minLines = if (multiline) 3 else 1,
        enabled = !state.saving,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text
            ),
        modifier =
            Modifier
                .fillMaxWidth()
    )
}

@Composable
internal fun Toggle(
    key: String,
    label: Int,
    state: EditorUiState,
    onChange: (String, String) -> Unit
) {
    val description = stringResource(label)
    Row(
        Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            description,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
        )
        Switch(
            modifier = Modifier.semantics { contentDescription = description },
            checked = state.values[key].toBoolean(),
            enabled = !state.saving,
            onCheckedChange = {
                onChange(key, it.toString())
            }
        )
    }
}

@Composable
internal fun FuelChoice(state: EditorUiState, onChange: (String, String) -> Unit) {
    ChoiceField(
        stringResource(R.string.fuel_type),
        state.values["fuelType"].orEmpty(),
        FuelType.entries.map {
            it.name to stringResource(fuelLabel(it))
        }
    ) {
        onChange("fuelType", it)
    }
}

@Composable
internal fun CategoryChoice(state: EditorUiState, onChange: (String, String) -> Unit) {
    ChoiceField(
        stringResource(R.string.category),
        state.values["category"].orEmpty(),
        Category.entries.map {
            it.name to stringResource(categoryLabel(it))
        }
    ) {
        onChange("category", it)
    }
}

@Composable
internal fun DistanceChoice(state: EditorUiState, onChange: (String, String) -> Unit) {
    ChoiceField(
        stringResource(R.string.distance_unit),
        state.values["distanceUnit"].orEmpty(),
        listOf("km" to stringResource(R.string.km), "mi" to stringResource(R.string.mi))
    ) {
        onChange("distanceUnit", it)
    }
}
