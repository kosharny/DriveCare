package com.example.drivecare.feature.journal

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drivecare.R
import com.example.drivecare.core.model.*
import com.example.drivecare.core.ui.*
import com.example.drivecare.domain.kindLabel
import java.io.File

@Composable
fun JournalRoute(onAddVehicle: () -> Unit, onAdd: (String) -> Unit, onRecord: (String) -> Unit) {
    val vm: JournalViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val kind by vm.kind.collectAsStateWithLifecycle()
    val start by vm.start.collectAsStateWithLifecycle()
    val end by vm.end.collectAsStateWithLifecycle()
    val records by vm.filtered.collectAsStateWithLifecycle()
    if (state.vehicle == null) {
        EmptyVehicle(onAddVehicle, Artwork.JOURNAL)
        return
    }
    JournalScreen(state, query, kind, start, end, records, vm::filter, onAdd, onRecord)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalScreen(
    state: FeatureUiState,
    query: String,
    kind: String,
    start: String,
    end: String,
    records: List<JournalRecord>?,
    onFilter: (String, String) -> Unit,
    onAdd: (String) -> Unit,
    onRecord: (String) -> Unit
) {
    Box(
        Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .widthIn(max = 1100.dp)
                    .fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "artwork") { ScreenArtwork(Artwork.JOURNAL) }
            item(key = "filters") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "fuel" to R.string.add_fuel,
                            "expense" to R.string.add_expense,
                            "service" to R.string.log_service
                        )
                            .forEach { (mode, label) ->
                                FilledTonalButton(
                                    onClick = {
                                        onAdd(mode)
                                    },
                                    enabled = state.vehicle?.archived == false
                                ) {
                                    Text(stringResource(label))
                                }
                            }
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            onFilter("query", it)
                        },
                        label = {
                            Text(stringResource(R.string.search))
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                    )
                    ChoiceField(
                        stringResource(R.string.record_type),
                        kind,
                        listOf("all" to stringResource(R.string.all_types)) +
                            RecordKind.entries.map {
                                it.name to stringResource(kindLabel(it))
                            }
                    ) {
                        onFilter("kind", it)
                    }
                    OutlinedTextField(
                        value = start,
                        onValueChange = {
                            onFilter("start", it)
                        },
                        label = {
                            Text(stringResource(R.string.start_date))
                        },
                        placeholder = {
                            Text("YYYY-MM-DD")
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = {
                            onFilter("end", it)
                        },
                        label = {
                            Text(stringResource(R.string.end_date))
                        },
                        placeholder = {
                            Text("YYYY-MM-DD")
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                    )
                    if (records == null) {
                        Text(
                            stringResource(R.string.invalid_fields),
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (records.isEmpty()) {
                        Text(stringResource(R.string.empty_journal))
                    }
                }
            }
            items(records.orEmpty(), key = {
                it.id
            }, contentType = {
                it.kind
            }) { record ->
                Card(
                    onClick = {
                        onRecord(record.id)
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        Modifier
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            record.title.ifBlank {
                                stringResource(kindLabel(record.kind))
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            stringResource(kindLabel(record.kind)) +
                                " · " +
                                date(record.date(state.zone).toString(), appLocale())
                        )
                        record.odometerKm?.let {
                            Text(distance(it, state.prefs.distanceUnit))
                        }
                        if (
                            record.kind in
                            listOf(RecordKind.FUEL, RecordKind.SERVICE, RecordKind.EXPENSE)
                        ) {
                            Text(
                                money(record.amountMinor, record.currency, appLocale()),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        record.liters?.let {
                            Text(volume(it.decimal(), state.prefs.volumeUnit))
                        }
                        record.estimatedConsumptionLPer100Km?.let { assumed ->
                            Text(stringResource(R.string.assumed_consumption_value, assumed))
                            record.liters?.let { liters ->
                                Text(
                                    stringResource(
                                        R.string.estimated_fuel_distance,
                                        distance(
                                            com.example.drivecare.domain.FuelMileage.distanceKm(
                                                liters.decimal(),
                                                assumed.decimal()
                                            ).toPlainString(),
                                            state.prefs.distanceUnit
                                        )
                                    )
                                )
                            }
                            Text(stringResource(R.string.estimated_mileage_warning))
                        }
                        if (record.stationOrWorkshop.isNotBlank()) Text(record.stationOrWorkshop)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordDetailRoute(id: String, onEdit: (String) -> Unit, onDeleted: () -> Unit) {
    val vm: JournalViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val record =
        state.snapshot.records.find {
            it.id == id
        }
    var deletion by rememberSaveable {
        mutableStateOf(false)
    }
    var attachmentError by remember {
        mutableStateOf(false)
    }
    val context = LocalContext.current
    PageColumn(Artwork.JOURNAL) {
        if (record != null) {
            InfoCard(
                record.title.ifBlank {
                    stringResource(kindLabel(record.kind))
                },
                date(record.date(state.zone).toString(), appLocale())
            ) {
                Text(stringResource(kindLabel(record.kind)))
                record.odometerKm?.let {
                    Text(distance(it, state.prefs.distanceUnit))
                }
                if (record.kind in
                    listOf(
                        RecordKind.FUEL,
                        RecordKind.SERVICE,
                        RecordKind.EXPENSE
                    )
                ) {
                    Text(money(record.amountMinor, record.currency, appLocale()))
                }
                record.liters?.let {
                    Text(volume(it.decimal(), state.prefs.volumeUnit))
                }
                record.fuelType?.let {
                    Text(stringResource(fuelLabel(it)))
                }
                if (record.fullTank) Text(stringResource(R.string.full_tank))
                if (record.missingFill) Text(stringResource(R.string.missing_fill))
                if (record.kind in
                    listOf(RecordKind.SERVICE, RecordKind.EXPENSE)
                ) {
                    Text(stringResource(categoryLabel(record.category)))
                }
                if (record.kind == RecordKind.FUEL &&
                    record.odometerKm == null
                ) {
                    Text(stringResource(R.string.mileage_not_recorded))
                }
                record.estimatedConsumptionLPer100Km?.let { assumed ->
                    Text(stringResource(R.string.assumed_consumption_value, assumed))
                    record.liters?.let { liters ->
                        Text(
                            stringResource(
                                R.string.estimated_fuel_distance,
                                distance(
                                    com.example.drivecare.domain.FuelMileage.distanceKm(
                                        liters.decimal(),
                                        assumed.decimal()
                                    ).toPlainString(),
                                    state.prefs.distanceUnit
                                )
                            )
                        )
                    }
                    Text(stringResource(R.string.estimated_mileage_warning))
                }
                if (record.stationOrWorkshop.isNotBlank()) Text(record.stationOrWorkshop)
                Text(record.notes)
            }
            state.snapshot.attachments
                .filter {
                    it.recordId == id
                }
                .forEach { attachment ->
                    OutlinedButton(
                        onClick = {
                            try {
                                val uri =
                                    FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.files",
                                        File(context.filesDir, "attachments/${attachment.path}")
                                    )
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW)
                                        .setDataAndType(uri, attachment.mimeType)
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                )
                            } catch (_: Exception) {
                                attachmentError = true
                            }
                        }
                    ) {
                        Text(stringResource(R.string.view_attachment))
                    }
                }
            Button(
                onClick = {
                    onEdit(record.kind.name.lowercase())
                },
                enabled = state.vehicle?.archived == false
            ) {
                Text(stringResource(R.string.edit))
            }
            TextButton(onClick = {
                deletion = true
            }) {
                Text(stringResource(R.string.delete))
            }
        }
    }
    Message(state.error, vm::acknowledge)
    Message(if (attachmentError) R.string.operation_error else null) {
        attachmentError = false
    }
    if (deletion && record != null) {
        DeleteConfirmation(
            onDismiss = {
                deletion = false
            },
            onConfirm = {
                deletion = false
                vm.delete(record)
                onDeleted()
            }
        )
    }
}
