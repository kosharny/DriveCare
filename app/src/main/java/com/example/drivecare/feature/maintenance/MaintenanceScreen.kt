package com.example.drivecare.feature.maintenance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drivecare.R
import com.example.drivecare.core.model.*
import com.example.drivecare.core.ui.*
import com.example.drivecare.domain.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields

@Composable
fun MaintenanceRoute(
    onAddVehicle: () -> Unit,
    onAddPlan: () -> Unit,
    onPlan: (String) -> Unit,
    onRecord: (String) -> Unit
) {
    val vm: MaintenanceViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val month by vm.month.collectAsStateWithLifecycle()
    val selected by vm.selectedDate.collectAsStateWithLifecycle()
    val agenda by vm.agenda.collectAsStateWithLifecycle()
    if (state.vehicle == null) {
        EmptyVehicle(onAddVehicle, Artwork.MAINTENANCE)
        return
    }
    MaintenanceScreen(
        state,
        YearMonth.parse(month),
        LocalDate.parse(selected),
        agenda,
        vm::moveMonth,
        vm::selectDate,
        vm::today,
        vm::showAgenda,
        onAddPlan,
        onPlan,
        onRecord
    )
}

@Composable
fun MaintenanceScreen(
    state: FeatureUiState,
    month: YearMonth,
    selected: LocalDate,
    agenda: Boolean,
    onMove: (Long) -> Unit,
    onDay: (String) -> Unit,
    onToday: () -> Unit,
    onAgenda: (Boolean) -> Unit,
    onAdd: () -> Unit,
    onPlan: (String) -> Unit,
    onRecord: (String) -> Unit
) {
    PageColumn(Artwork.MAINTENANCE) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                selected = !agenda,
                onClick = {
                    onAgenda(false)
                },
                label = {
                    Text(stringResource(R.string.calendar))
                }
            )
            FilterChip(
                selected = agenda,
                onClick = {
                    onAgenda(true)
                },
                label = {
                    Text(stringResource(R.string.agenda))
                }
            )
        }
        Button(onClick = onAdd, enabled = state.vehicle?.archived == false) {
            Text(stringResource(R.string.add_plan))
        }
        if (!agenda) {
            MonthCalendar(month, selected, state.plans, state.records, onMove, onDay, onToday)
            Text(
                date(selected.toString(), appLocale()),
                style = MaterialTheme.typography.titleLarge
            )
            val plans =
                state.plans.filter {
                    it.dueDate == selected.toString() && !it.completed
                }
            val services =
                state.records.filter {
                    it.kind == RecordKind.SERVICE && it.serviceDate == selected.toString()
                }
            if (plans.isEmpty() && services.isEmpty()) Text(stringResource(R.string.no_events))
            plans.forEach { plan ->
                PlanCard(plan, state.statuses.getValue(plan.id), state.prefs.distanceUnit) {
                    onPlan(plan.id)
                }
            }
            services.forEach { record ->
                OutlinedCard(
                    onClick = {
                        onRecord(record.id)
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {
                    Column(
                        Modifier
                            .padding(16.dp)
                    ) {
                        Text(
                            stringResource(R.string.completed),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(record.title, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Text(
                stringResource(R.string.distance_plans),
                style = MaterialTheme.typography.titleMedium
            )
        }
        val visible =
            if (agenda) {
                state.plans
            } else {
                state.plans.filter {
                    it.dueDate == null
                }
            }
        if (state.plans.isEmpty()) Text(stringResource(R.string.empty_plans))
        visible.forEach { plan ->
            PlanCard(plan, state.statuses.getValue(plan.id), state.prefs.distanceUnit) {
                onPlan(plan.id)
            }
        }
        if (agenda) {
            state.records
                .filter {
                    it.kind == RecordKind.SERVICE
                }
                .forEach { record ->
                    OutlinedCard(
                        onClick = {
                            onRecord(record.id)
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                    ) {
                        Column(
                            Modifier
                                .padding(16.dp)
                        ) {
                            Text(
                                stringResource(R.string.completed) +
                                    " · " +
                                    date(record.serviceDate!!, appLocale())
                            )
                            Text(record.title)
                        }
                    }
                }
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    selected: LocalDate,
    plans: List<MaintenancePlan>,
    records: List<JournalRecord>,
    onMove: (Long) -> Unit,
    onDay: (String) -> Unit,
    onToday: () -> Unit
) {
    val locale = appLocale()
    val firstWeekday = WeekFields.of(locale).firstDayOfWeek
    val offset = (month.atDay(1).dayOfWeek.value - firstWeekday.value + 7) % 7
    InfoCard(month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))) {
        Row(
            Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = {
                onMove(-1)
            }) {
                Text(stringResource(R.string.previous))
            }
            TextButton(onClick = onToday) {
                Text(stringResource(R.string.today))
            }
            TextButton(onClick = {
                onMove(1)
            }) {
                Text(stringResource(R.string.next))
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
        ) {
            repeat(7) { index ->
                Text(
                    firstWeekday.plus(index.toLong()).getDisplayName(TextStyle.SHORT, locale),
                    modifier =
                        Modifier
                            .weight(1f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        repeat((offset + month.lengthOfMonth() + 6) / 7) { week ->
            Row(
                Modifier
                    .fillMaxWidth()
            ) {
                repeat(7) { weekday ->
                    val day = week * 7 + weekday - offset + 1
                    if (day !in 1..month.lengthOfMonth()) {
                        Spacer(
                            Modifier
                                .weight(1f)
                        )
                    } else {
                        val date = month.atDay(day)
                        val planned =
                            plans.any {
                                it.dueDate == date.toString() && !it.completed
                            }
                        val completed =
                            records.any {
                                it.serviceDate == date.toString() && it.kind == RecordKind.SERVICE
                            }
                        val description =
                            listOf(
                                date.toString(),
                                if (planned) stringResource(R.string.planned) else "",
                                if (completed) stringResource(R.string.completed) else ""
                            )
                                .filter {
                                    it.isNotEmpty()
                                }
                                .joinToString(", ")
                        Surface(
                            onClick = {
                                onDay(date.toString())
                            },
                            color =
                                if (date == selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            shape = MaterialTheme.shapes.small,
                            border =
                                if (date == selected) {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    null
                                },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .heightIn(min = 56.dp).semantics {
                                        contentDescription = description
                                    }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier =
                                    Modifier
                                        .padding(vertical = 8.dp)
                            ) {
                                Text(day.toString(), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    (if (planned) "●" else "") + (if (completed) "✓" else ""),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
        Text(
            "● " + stringResource(R.string.planned) + "   ✓ " + stringResource(R.string.completed),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun PlanCard(plan: MaintenancePlan, status: PlanStatus, distanceUnit: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(plan.title, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(statusLabel(status.status)),
                color =
                    if (status.status == DueStatus.OVERDUE) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                style = MaterialTheme.typography.labelLarge
            )
            val day =
                plan.dueDate?.let {
                    date(it, appLocale())
                }
            val mileage =
                plan.dueKm?.let {
                    distance(it, distanceUnit)
                }
            if (day != null && mileage != null) {
                Text(stringResource(R.string.either_threshold, day, mileage))
            } else if (day != null) {
                Text(stringResource(R.string.due_by, day))
            } else if (mileage != null) {
                Text(stringResource(R.string.due_at, mileage))
            } else {
                Text(stringResource(R.string.not_enough_data))
            }
            if (status.dateTriggered) {
                Text(
                    stringResource(R.string.date_cause),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (status.distanceTriggered) {
                Text(
                    stringResource(R.string.distance_cause),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (status.mileageStale && plan.dueKm != null) {
                Text(
                    stringResource(R.string.mileage_stale),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun PlanDetailRoute(id: String, onEdit: () -> Unit, onComplete: () -> Unit, onDeleted: () -> Unit) {
    val vm: MaintenanceViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val plan =
        state.snapshot.plans.find {
            it.id == id
        }
    var delete by rememberSaveable {
        mutableStateOf(false)
    }
    PageColumn(Artwork.MAINTENANCE) {
        if (plan != null) {
            PlanCard(
                plan,
                state.statuses[plan.id]
                    ?: MaintenanceRules.status(plan, null, state.today, state.zone),
                state.prefs.distanceUnit
            ) {
            }
            Text(stringResource(categoryLabel(plan.category)))
            Text(plan.notes)
            plan.estimatedMinor?.let {
                Text(
                    stringResource(R.string.estimated_cost) +
                        ": " +
                        money(it, plan.currency, appLocale())
                )
            }
            Text(
                stringResource(R.string.recurrence_help),
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = onComplete,
                enabled = !plan.completed && state.vehicle?.archived == false
            ) {
                Text(stringResource(R.string.complete))
            }
            OutlinedButton(onClick = onEdit, enabled = state.vehicle?.archived == false) {
                Text(stringResource(R.string.edit))
            }
            TextButton(
                onClick = {
                    vm.togglePause(plan)
                },
                enabled = state.vehicle?.archived == false
            ) {
                Text(stringResource(if (plan.paused) R.string.resume else R.string.pause))
            }
            TextButton(onClick = {
                delete = true
            }) {
                Text(stringResource(R.string.delete))
            }
        }
    }
    Message(state.error, vm::acknowledge)
    if (delete && plan != null) {
        DeleteConfirmation(
            onDismiss = {
                delete = false
            },
            onConfirm = {
                delete = false
                vm.delete(plan)
                onDeleted()
            }
        )
    }
}
