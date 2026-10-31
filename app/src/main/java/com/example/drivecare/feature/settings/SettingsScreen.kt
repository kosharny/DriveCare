package com.example.drivecare.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drivecare.R
import com.example.drivecare.core.ui.*

@Composable
fun SettingsRoute(onPro: () -> Unit, onRestored: () -> Unit) {
    val vm: SettingsViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val operation by vm.operation.collectAsStateWithLifecycle()
    var confirmation by rememberSaveable {
        mutableStateOf("")
    }
    var currency by rememberSaveable(state.prefs.currency) {
        mutableStateOf(state.prefs.currency)
    }
    var zone by
        rememberSaveable(state.prefs.reportingZone) {
            mutableStateOf(state.prefs.reportingZone)
        }
    val csv =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
            if (it != null) vm.export(it, "csv")
        }
    val backup =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip")
        ) {
            if (it != null) vm.export(it, "backup")
        }
    val restore =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
            if (it != null) vm.restore(it)
        }
    PageColumn(Artwork.SETTINGS) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium)
        ChoiceField(
            stringResource(R.string.language),
            AppCompatDelegate.getApplicationLocales().toLanguageTags(),
            listOf(
                "" to stringResource(R.string.system_default),
                "en" to "English",
                "ru" to "Русский",
                "uk" to "Українська"
            )
        ) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it))
        }
        ChoiceField(
            stringResource(R.string.theme),
            state.prefs.theme,
            listOf(
                "system" to stringResource(R.string.system_default),
                "light" to stringResource(R.string.light),
                "dark" to stringResource(R.string.dark)
            )
        ) {
            vm.setting("theme", it)
        }
        ChoiceField(
            stringResource(R.string.distance_unit),
            state.prefs.distanceUnit,
            listOf("km" to stringResource(R.string.km), "mi" to stringResource(R.string.mi))
        ) {
            vm.setting("distance", it)
        }
        ChoiceField(
            stringResource(R.string.volume_unit),
            state.prefs.volumeUnit,
            listOf(
                "L" to stringResource(R.string.liters),
                "US gal" to stringResource(R.string.us_gallons)
            )
        ) {
            vm.setting("volume", it)
        }
        OutlinedTextField(
            value = currency,
            onValueChange = {
                currency = it
            },
            label = {
                Text(stringResource(R.string.currency))
            },
            modifier =
                Modifier
                    .fillMaxWidth()
        )
        TextButton(onClick = {
            vm.setting("currency", currency)
        }) {
            Text(stringResource(R.string.save))
        }
        OutlinedTextField(
            value = zone,
            onValueChange = {
                zone = it
            },
            label = {
                Text(stringResource(R.string.reporting_zone))
            },
            modifier =
                Modifier
                    .fillMaxWidth()
        )
        TextButton(onClick = {
            vm.setting("zone", zone)
        }) {
            Text(stringResource(R.string.save))
        }
        InfoCard(stringResource(R.string.backup), stringResource(R.string.backup_help)) {
            OutlinedButton(
                onClick = {
                    csv.launch("DriveCare-records.csv")
                },
                enabled = !operation.busy
            ) {
                Text(stringResource(R.string.export_csv))
            }
            OutlinedButton(
                onClick = {
                    backup.launch("DriveCare-backup.zip")
                },
                enabled = !operation.busy
            ) {
                Text(stringResource(R.string.backup))
            }
            OutlinedButton(onClick = {
                confirmation = "restore"
            }, enabled = !operation.busy) {
                Text(stringResource(R.string.restore_backup))
            }
        }
        Button(onClick = onPro) {
            Text(
                stringResource(if (state.billing.pro) R.string.pro_active else R.string.unlock_pro)
            )
        }
        TextButton(onClick = vm::revokeConsent) {
            Text(stringResource(R.string.revoke_consent))
        }
        TextButton(onClick = {
            confirmation = "ai"
        }, enabled = !operation.busy) {
            Text(stringResource(R.string.clear_ai))
        }
        TextButton(onClick = {
            confirmation = "delete"
        }, enabled = !operation.busy) {
            Text(stringResource(R.string.delete_all), color = MaterialTheme.colorScheme.error)
        }
        if (operation.busy) {
            LinearProgressIndicator(
                Modifier
                    .fillMaxWidth()
            )
        }
        InfoCard(stringResource(R.string.privacy), stringResource(R.string.privacy_body))
    }
    if (confirmation.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                confirmation = ""
            },
            title = {
                Text(
                    stringResource(
                        if (confirmation == "restore") {
                            R.string.restore_confirm
                        } else {
                            R.string.confirm_delete
                        }
                    )
                )
            },
            text = {
                Text(
                    stringResource(
                        if (confirmation == "restore") {
                            R.string.backup_help
                        } else {
                            R.string.delete_warning
                        }
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (confirmation) {
                            "restore" ->
                                restore.launch(
                                    arrayOf("application/zip", "application/octet-stream")
                                )

                            "ai" -> vm.clearAi()

                            "delete" -> vm.deleteAll()
                        }
                        confirmation = ""
                    }
                ) {
                    Text(stringResource(R.string.continue_action))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmation = ""
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    Message(state.error, vm::acknowledge)
    Message(operation.message) {
        val restored = operation.restored
        vm.acknowledgeOperation()
        if (restored) onRestored()
    }
}
