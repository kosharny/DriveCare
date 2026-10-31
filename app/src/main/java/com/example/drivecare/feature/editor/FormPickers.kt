package com.example.drivecare.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.drivecare.R
import com.example.drivecare.core.ui.appLocale
import com.example.drivecare.core.ui.date
import com.example.drivecare.data.catalogue.VehicleSuggestions
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun SearchChoiceField(
    label: String,
    selected: String,
    choices: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    allowManual: Boolean = false,
    enabled: Boolean = true,
    numeric: Boolean = false
) {
    var open by rememberSaveable { mutableStateOf(false) }
    PickerButton(label, choices.find { it.first == selected }?.second ?: selected, enabled) {
        open =
            true
    }
    if (open) {
        var query by rememberSaveable { mutableStateOf("") }
        val matches =
            remember(choices, query) {
                choices.filter {
                    VehicleSuggestions.matches(it.second, query) ||
                        VehicleSuggestions.matches(it.first, query)
                }
            }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it.take(250) },
                        label = { Text(stringResource(R.string.search_hint)) },
                        singleLine = true,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    if (numeric) {
                                        KeyboardType.Number
                                    } else {
                                        KeyboardType.Text
                                    }
                            ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(matches, key = { it.first }) { option ->
                            TextButton(onClick = {
                                onSelected(option.first)
                                open = false
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(option.second, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    if (matches.isEmpty()) Text(stringResource(R.string.no_matches))
                }
            },
            confirmButton = {
                if (allowManual && query.isNotBlank()) {
                    TextButton(onClick = {
                        onSelected(query.trim())
                        open = false
                    }) { Text(stringResource(R.string.use_search_value, query.trim())) }
                }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
fun PickerButton(label: String, value: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value.ifBlank {
                    stringResource(R.string.choose_value)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text("⌄", style = MaterialTheme.typography.titleLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateChoice(
    key: String,
    label: Int,
    state: EditorUiState,
    onChange: (String, String) -> Unit,
    optional: Boolean = false
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val value = state.values[key].orEmpty()
    val locale = appLocale()
    PickerButton(
        stringResource(label),
        if (value.isBlank()) "" else runCatching { date(value, locale) }.getOrDefault(value),
        !state.saving
    ) {
        open =
            true
    }
    if (open) {
        val initial =
            runCatching { LocalDate.parse(value) }.getOrNull()
                ?.takeIf { it.year in 1..9999 } ?: LocalDate.now()
        val picker =
            rememberDatePickerState(
                initialSelectedDateMillis =
                    initial.atStartOfDay(
                        ZoneOffset.UTC
                    ).toInstant().toEpochMilli(),
                yearRange =
                    1..9999
            )
        DatePickerDialog(onDismissRequest = { open = false }, confirmButton = {
            TextButton(onClick = {
                picker.selectedDateMillis?.let {
                    onChange(
                        key,
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    )
                }
                open = false
            }) { Text(stringResource(R.string.save)) }
        }, dismissButton = {
            Row {
                if (optional) {
                    TextButton(onClick = {
                        onChange(key, "")
                        open = false
                    }) { Text(stringResource(R.string.optional_clear)) }
                }
                TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) }
            }
        }) { DatePicker(state = picker) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeChoice(state: EditorUiState, onChange: (String, String) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    PickerButton(stringResource(R.string.time), state.values["time"].orEmpty(), !state.saving) {
        open =
            true
    }
    if (open) {
        val initial =
            runCatching { LocalTime.parse(state.values["time"].orEmpty()) }
                .getOrDefault(LocalTime.NOON)
        val picker =
            rememberTimePickerState(
                initialHour = initial.hour,
                initialMinute = initial.minute,
                is24Hour = true
            )
        AlertDialog(
            onDismissRequest = {
                open = false
            },
            title = { Text(stringResource(R.string.time)) },
            text = { TimeInput(state = picker) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(
                        "time",
                        LocalTime.of(
                            picker.hour,
                            picker.minute
                        ).format(DateTimeFormatter.ofPattern("HH:mm"))
                    )
                    open = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
fun OptionalSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            Text(title, modifier = Modifier.weight(1f))
            Text(if (expanded) "−" else "+")
        }
        if (expanded) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content
            )
        }
    }
}
