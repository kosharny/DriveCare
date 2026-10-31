package com.example.drivecare.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.drivecare.R
import com.example.drivecare.core.model.Category
import com.example.drivecare.core.ui.*
import com.example.drivecare.feature.aigarage.AiGarageRoute
import com.example.drivecare.feature.editor.EditorRoute
import com.example.drivecare.feature.garage.GarageRoute
import com.example.drivecare.feature.journal.JournalRoute
import com.example.drivecare.feature.journal.RecordDetailRoute
import com.example.drivecare.feature.maintenance.MaintenanceRoute
import com.example.drivecare.feature.maintenance.PlanDetailRoute
import com.example.drivecare.feature.overview.OverviewRoute
import com.example.drivecare.feature.paywall.PaywallRoute
import com.example.drivecare.feature.settings.SettingsRoute
import com.example.drivecare.feature.statistics.StatisticsRoute
import com.example.drivecare.ui.theme.DriveCareTheme
import kotlinx.serialization.Serializable

@Serializable data object HomeKey : NavKey

@Serializable data object GarageKey : NavKey

@Serializable data object SettingsKey : NavKey

@Serializable data object PaywallKey : NavKey

@Serializable data object TaskSetupKey : NavKey

@Serializable data class PlanKey(val id: String) : NavKey

@Serializable data class RecordKey(val id: String) : NavKey

@Serializable
data class EditorKey(
    val mode: String,
    val id: String = "",
    val vehicleId: String = "",
    val planId: String = "",
    val draftTitle: String = "",
    val draftNotes: String = ""
) : NavKey

data class NavigationTab(val id: String, val label: Int, val icon: Int)

private val tabs =
    listOf(
        NavigationTab("overview", R.string.overview, R.drawable.ic_dashboard),
        NavigationTab("maintenance", R.string.maintenance, R.drawable.ic_calendar),
        NavigationTab("journal", R.string.journal, R.drawable.ic_journal),
        NavigationTab("statistics", R.string.statistics, R.drawable.ic_chart),
        NavigationTab("ai", R.string.ai_garage, R.drawable.ic_spark)
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveCareApp(
    onPurchase: () -> Unit,
    reminderVehicleId: String?,
    reminderPlanId: String?,
    systemSplashGone: Boolean = true
) {
    val vm: AppViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val showingIntro by vm.showingIntro.collectAsStateWithLifecycle()
    LaunchedEffect(systemSplashGone, showingIntro) {
        if (systemSplashGone && showingIntro) {
            withFrameNanos { }
            vm.onBrandVisible()
        }
    }
    val stack = rememberNavBackStack(HomeKey)
    var tab by rememberSaveable {
        mutableStateOf("overview")
    }
    val holder = rememberSaveableStateHolder()
    val current = stack.lastOrNull() ?: HomeKey
    val back: () -> Unit = {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
    val addVehicle = {
        stack.add(EditorKey("vehicle"))
        Unit
    }
    val vehicleId = state.vehicle?.id.orEmpty()
    val editor: (String) -> Unit = { mode ->
        stack.add(EditorKey(mode, vehicleId = vehicleId))
    }
    LaunchedEffect(reminderVehicleId, reminderPlanId, state.loaded) {
        if (state.loaded && reminderVehicleId != null && reminderPlanId != null) {
            vm.selectVehicle(reminderVehicleId)
            if (stack.lastOrNull() != PlanKey(reminderPlanId)) stack.add(PlanKey(reminderPlanId))
        }
    }
    DriveCareTheme(theme = state.prefs.theme) {
        if (!state.loaded || showingIntro) {
            BrandLoadingScreen(error = state.error, onRetry = vm::retryLoading)
            return@DriveCareTheme
        }
        if (!state.prefs.onboardingDone) {
            OnboardingScreen(vm::finishOnboarding)
            return@DriveCareTheme
        }
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
        ) {
            val wide = maxWidth >= 720.dp
            val home = current == HomeKey
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                if (home) {
                                    "DriveCare"
                                } else {
                                    stringResource(
                                        when (current) {
                                            GarageKey -> R.string.garage
                                            SettingsKey -> R.string.settings
                                            PaywallKey -> R.string.pro_title
                                            else -> R.string.app_name
                                        }
                                    )
                                }
                            )
                        },
                        navigationIcon = {
                            if (!home) {
                                IconButton(onClick = back) {
                                    Icon(
                                        painterResource(R.drawable.ic_back),
                                        stringResource(R.string.back)
                                    )
                                }
                            }
                        },
                        actions = {
                            if (current != SettingsKey) {
                                IconButton(onClick = {
                                    stack.add(SettingsKey)
                                }) {
                                    Icon(
                                        painterResource(R.drawable.ic_settings),
                                        stringResource(R.string.settings)
                                    )
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    if (home && !wide) {
                        NavigationBar {
                            tabs.forEach { item ->
                                NavigationBarItem(
                                    selected = tab == item.id,
                                    onClick = {
                                        tab = item.id
                                    },
                                    icon = {
                                        Icon(painterResource(item.icon), contentDescription = null)
                                    },
                                    label = {
                                        Text(stringResource(item.label), maxLines = 2)
                                    }
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (home && wide) {
                        NavigationRail(
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            tabs.forEach { item ->
                                NavigationRailItem(
                                    selected = tab == item.id,
                                    onClick = {
                                        tab = item.id
                                    },
                                    icon = {
                                        Icon(painterResource(item.icon), contentDescription = null)
                                    },
                                    label = {
                                        Text(stringResource(item.label))
                                    }
                                )
                            }
                        }
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (home) {
                            TextButton(
                                onClick = {
                                    stack.add(GarageKey)
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                            ) {
                                Icon(painterResource(R.drawable.ic_car), contentDescription = null)
                                Spacer(
                                    Modifier
                                        .width(8.dp)
                                )
                                Text(state.vehicle?.displayName ?: stringResource(R.string.garage))
                            }
                        }
                        NavDisplay(
                            backStack = stack,
                            onBack = back,
                            entryDecorators =
                                listOf(
                                    rememberSaveableStateHolderNavEntryDecorator(),
                                    rememberViewModelStoreNavEntryDecorator()
                                ),
                            entryProvider =
                                entryProvider {
                                    entry<HomeKey> {
                                        if (
                                            !state.billing.pro &&
                                            state.billing.checked &&
                                            state.snapshot.vehicles.count {
                                                !it.archived
                                            } > 1
                                        ) {
                                            GarageRoute(
                                                addVehicle,
                                                {
                                                    stack.add(EditorKey("vehicle", id = it))
                                                },
                                                {
                                                },
                                                {
                                                    stack.add(PaywallKey)
                                                }
                                            )
                                        } else {
                                            holder.SaveableStateProvider(tab) {
                                                when (tab) {
                                                    "overview" ->
                                                        OverviewRoute(
                                                            addVehicle,
                                                            editor,
                                                            {
                                                                stack.add(PlanKey(it))
                                                            },
                                                            {
                                                                tab = "ai"
                                                            }
                                                        )

                                                    "maintenance" ->
                                                        MaintenanceRoute(
                                                            addVehicle,
                                                            {
                                                                editor("plan")
                                                            },
                                                            {
                                                                stack.add(PlanKey(it))
                                                            },
                                                            {
                                                                stack.add(RecordKey(it))
                                                            }
                                                        )

                                                    "journal" ->
                                                        JournalRoute(
                                                            addVehicle,
                                                            editor,
                                                            {
                                                                stack.add(RecordKey(it))
                                                            }
                                                        )

                                                    "statistics" ->
                                                        StatisticsRoute(
                                                            addVehicle,
                                                            {
                                                                stack.add(PaywallKey)
                                                            }
                                                        )

                                                    "ai" ->
                                                        AiGarageRoute(addVehicle) {
                                                                mode,
                                                                title,
                                                                notes
                                                            ->
                                                            stack.add(
                                                                EditorKey(
                                                                    mode,
                                                                    vehicleId = vehicleId,
                                                                    draftTitle = title,
                                                                    draftNotes = notes
                                                                )
                                                            )
                                                        }
                                                }
                                            }
                                        }
                                    }
                                    entry<GarageKey> {
                                        GarageRoute(
                                            addVehicle,
                                            {
                                                stack.add(EditorKey("vehicle", id = it))
                                            },
                                            back,
                                            {
                                                stack.add(PaywallKey)
                                            }
                                        )
                                    }
                                    entry<SettingsKey> {
                                        SettingsRoute(
                                            onPro = { stack.add(PaywallKey) },
                                            onRestored = {
                                                stack.clear()
                                                stack.add(HomeKey)
                                            }
                                        )
                                    }
                                    entry<PaywallKey> {
                                        PaywallRoute(onPurchase)
                                    }
                                    entry<EditorKey> { key ->
                                        EditorRoute(
                                            key.mode,
                                            key.id,
                                            key.vehicleId,
                                            key.planId,
                                            key.draftTitle,
                                            key.draftNotes,
                                            onSaved = {
                                                back()
                                                if (key.mode == "vehicle" && key.id.isBlank()) {
                                                    stack.add(TaskSetupKey)
                                                }
                                            },
                                            onBack = back
                                        )
                                    }
                                    entry<PlanKey> { key ->
                                        val planVehicle =
                                            state.snapshot.plans.find {
                                                it.id == key.id
                                            }?.vehicleId
                                                ?: vehicleId
                                        PlanDetailRoute(
                                            key.id,
                                            onEdit = {
                                                stack.add(
                                                    EditorKey(
                                                        "plan",
                                                        id = key.id,
                                                        vehicleId = planVehicle
                                                    )
                                                )
                                            },
                                            onComplete = {
                                                stack.add(
                                                    EditorKey(
                                                        "service",
                                                        vehicleId = planVehicle,
                                                        planId = key.id
                                                    )
                                                )
                                            },
                                            onDeleted = back
                                        )
                                    }
                                    entry<RecordKey> { key ->
                                        val record =
                                            state.snapshot.records.find {
                                                it.id == key.id
                                            }
                                        RecordDetailRoute(
                                            key.id,
                                            onEdit = { mode ->
                                                stack.add(
                                                    EditorKey(
                                                        mode,
                                                        id = key.id,
                                                        vehicleId = record?.vehicleId ?: vehicleId
                                                    )
                                                )
                                            },
                                            onDeleted = back
                                        )
                                    }
                                    entry<TaskSetupKey> {
                                        TaskSetupScreen(
                                            onCategory = { title ->
                                                stack.add(
                                                    EditorKey(
                                                        "plan",
                                                        vehicleId = vehicleId,
                                                        draftTitle = title
                                                    )
                                                )
                                            },
                                            onSkip = back
                                        )
                                    }
                                },
                            modifier =
                                Modifier
                                    .fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskSetupScreen(onCategory: (String) -> Unit, onSkip: () -> Unit) {
    PageColumn(Artwork.PLAN) {
        Text(
            stringResource(R.string.next_maintenance),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(stringResource(R.string.recurrence_help))
        Category.entries.take(8).forEach { category ->
            val title = stringResource(categoryLabel(category))
            OutlinedButton(onClick = {
                onCategory(title)
            }) {
                Text(title)
            }
        }
        Button(onClick = onSkip) {
            Text(stringResource(R.string.skip))
        }
    }
}

@Composable
private fun OnboardingScreen(onDone: () -> Unit) {
    var page by rememberSaveable {
        mutableIntStateOf(0)
    }
    val titles =
        listOf(
            R.string.onboarding_title_1,
            R.string.onboarding_title_2,
            R.string.onboarding_title_3
        )
    val bodies =
        listOf(R.string.onboarding_body_1, R.string.onboarding_body_2, R.string.onboarding_body_3)
    Surface(
        Modifier
            .fillMaxSize()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ScreenArtwork(
                    listOf(Artwork.ONBOARD_CARE, Artwork.ONBOARD_COSTS, Artwork.ONBOARD_AI)[page],
                    onboarding = true
                )
                Text(stringResource(titles[page]), style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(bodies[page]), style = MaterialTheme.typography.bodyLarge)
                Text("${page + 1} / 3", style = MaterialTheme.typography.labelLarge)
                Button(onClick = {
                    if (page < 2) page++ else onDone()
                }) {
                    Text(stringResource(if (page < 2) R.string.next else R.string.get_started))
                }
                TextButton(onClick = onDone) {
                    Text(stringResource(R.string.skip))
                }
            }
        }
    }
}
