package com.example.drivecare.feature.aigarage

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drivecare.R
import com.example.drivecare.core.model.*
import com.example.drivecare.core.ui.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiGarageRoute(onAddVehicle: () -> Unit, onDraft: (String, String, String) -> Unit) {
    val vm: AiGarageViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val ai by vm.aiState.collectAsStateWithLifecycle()
    val language =
        appLocale().language.takeIf {
            it in setOf("en", "ru", "uk")
        } ?: "en"
    var deletion by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    if (state.vehicle == null) {
        EmptyVehicle(onAddVehicle, artwork = null)
        return
    }
    PageColumn {
        InfoCard(
            stringResource(R.string.ai_context),
            "${state.vehicle!!.make} ${state.vehicle!!.model} · ${state.vehicle!!.year}"
        ) {
            val vehicle = state.vehicle!!
            if (vehicle.generation.isNotBlank()) Text(vehicle.generation)
            if (vehicle.engine.isNotBlank()) Text(vehicle.engine)
            if (vehicle.transmission.isNotBlank()) Text(vehicle.transmission)
            if (vehicle.market.isNotBlank()) Text(vehicle.market)
            Text(stringResource(fuelLabel(vehicle.fuelType)))
            state.reading?.odometerKm?.let {
                Text(
                    stringResource(R.string.last_mileage) +
                        ": " +
                        distance(it, state.prefs.distanceUnit)
                )
            }
            Text(stringResource(R.string.ai_grounding), style = MaterialTheme.typography.bodySmall)
        }
        if (!vm.configured) {
            Text(
                stringResource(R.string.ai_unavailable),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val shortcuts =
            listOf(
                Triple("weakPoints", R.string.weak_points, R.string.weak_question),
                Triple("maintenance", R.string.planning_help, R.string.planning_question),
                Triple("upgrades", R.string.upgrade_ideas, R.string.upgrade_question),
                Triple("records", R.string.record_question, R.string.record_question)
            )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            shortcuts.forEach { (mode, title, prompt) ->
                val text = stringResource(prompt)
                SuggestionChip(
                    onClick = {
                        vm.shortcut(mode, text)
                    },
                    label = {
                        Text(stringResource(title))
                    },
                    enabled = !ai.sending
                )
            }
        }
        OutlinedTextField(
            value = ai.question,
            onValueChange = vm::question,
            minLines = 3,
            label = {
                Text(stringResource(R.string.ai_question))
            },
            enabled = !ai.sending,
            modifier =
                Modifier
                    .fillMaxWidth()
        )
        Row(
            Modifier
                .fillMaxWidth()
        ) {
            Checkbox(
                checked = ai.includeTotals,
                onCheckedChange = vm::include,
                enabled = !ai.sending
            )
            Text(
                stringResource(R.string.include_summary),
                modifier =
                    Modifier
                        .padding(top = 12.dp)
                        .weight(1f)
            )
        }
        if (ai.sending) {
            LinearProgressIndicator(
                Modifier
                    .fillMaxWidth()
            )
            Text(stringResource(R.string.sending))
            OutlinedButton(onClick = vm::cancel) {
                Text(stringResource(R.string.cancel))
            }
        } else {
            Button(
                onClick = {
                    vm.send(language)
                },
                enabled = ai.question.isNotBlank() && vm.configured
            ) {
                Text(stringResource(R.string.send))
            }
        }
        ai.message?.let {
            Text(stringResource(it), color = MaterialTheme.colorScheme.error)
        }
        Text(stringResource(R.string.ai_limit_help), style = MaterialTheme.typography.bodySmall)
        ai.advice?.let { advice ->
            val conversation =
                state.snapshot.conversations.find {
                    it.id == ai.selectedId
                }
            if (conversation?.revision != state.vehicle?.revision) {
                Text(stringResource(R.string.stale_advice), color = MaterialTheme.colorScheme.error)
            }
            InfoCard(advice.summary) {
                if (advice.missingDetails.isNotEmpty()) {
                    Text(
                        stringResource(R.string.missing_details),
                        style = MaterialTheme.typography.titleSmall
                    )
                    advice.missingDetails.forEach {
                        Text("• $it")
                    }
                }
            }
            advice.items.forEach { item ->
                AdviceCard(item)
            }
            advice.draftActions.forEach { draft ->
                InfoCard(draft.title, draft.notes) {
                    Text(
                        stringResource(R.string.review_draft),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = {
                            onDraft("plan", draft.title, draft.notes)
                        },
                        enabled = state.vehicle?.archived == false
                    ) {
                        Text(stringResource(R.string.create_draft))
                    }
                }
            }
            OutlinedButton(
                onClick = {
                    onDraft("note", conversation?.question.orEmpty(), advice.summary)
                },
                enabled = state.vehicle?.archived == false
            ) {
                Text(stringResource(R.string.save_note))
            }
        }
        Text(stringResource(R.string.saved_advice), style = MaterialTheme.typography.titleLarge)
        state.snapshot.conversations
            .filter {
                it.vehicleId == state.vehicle?.id
            }
            .forEach { conversation ->
                InfoCard(
                    conversation.question,
                    "${conversation.language} · ${date(
                        java.time.Instant.ofEpochMilli(
                            conversation.timestamp
                        ).atZone(state.zone).toLocalDate().toString(),
                        appLocale()
                    )}"
                ) {
                    TextButton(onClick = {
                        vm.select(conversation.id)
                    }) {
                        Text(stringResource(R.string.view_details))
                    }
                    TextButton(onClick = {
                        deletion = conversation.id
                    }) {
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        InfoCard(
            stringResource(R.string.general_guide),
            stringResource(R.string.general_guide_body)
        )
    }
    if (ai.consentRequired) {
        AlertDialog(
            onDismissRequest = vm::dismissConsent,
            title = {
                Text(stringResource(R.string.ai_consent_title))
            },
            text = {
                Text(stringResource(R.string.ai_consent_body))
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.consentAndSend(language)
                }) {
                    Text(stringResource(R.string.continue_action))
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissConsent) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    Message(state.error, vm::acknowledge)
    deletion?.let { id ->
        DeleteConfirmation(
            onDismiss = {
                deletion = null
            },
            onConfirm = {
                deletion = null
                vm.delete(id)
            }
        )
    }
}

@Composable
private fun AdviceCard(item: AdviceItem) {
    InfoCard(item.title, item.explanation) {
        Text(
            stringResource(
                when (item.evidenceStatus) {
                    "generalGuidance" -> R.string.general_guide
                    "suppliedUserFact" -> R.string.supplied_fact
                    else -> R.string.requires_verification
                }
            ),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium
        )
        listOf(
            R.string.signs_to_watch to item.signsToWatch,
            R.string.next_steps to item.nextSteps,
            R.string.compatibility to item.compatibilityChecks
        )
            .forEach { (label, items) ->
                if (items.isNotEmpty()) {
                    Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
                    items.forEach {
                        Text("• $it")
                    }
                }
            }
    }
}
