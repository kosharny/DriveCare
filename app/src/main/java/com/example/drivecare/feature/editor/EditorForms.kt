package com.example.drivecare.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.drivecare.R
import com.example.drivecare.core.model.Category
import com.example.drivecare.core.ui.*
import java.time.LocalDate
import java.util.Currency
import java.util.Locale

@Composable
internal fun VehicleForm(
    state: EditorUiState,
    step: Int,
    onChange: (String, String) -> Unit,
    onAttach: () -> Unit
) {
    Text(
        stringResource(R.string.step_progress, step + 1, 2),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        stringResource(
            if (step ==
                0
            ) {
                R.string.vehicle_step_one
            } else {
                R.string.vehicle_step_two
            }
        ),
        style = MaterialTheme.typography.titleLarge
    )
    if (step == 0) {
        Text(stringResource(R.string.vehicle_identity_help))
        SearchChoiceField(
            stringResource(R.string.make),
            state.values["make"].orEmpty(),
            state.makes.map {
                it to
                    it
            },
            onSelected = { onChange("make", it) },
            allowManual = true,
            enabled = !state.saving
        )
        SearchChoiceField(
            stringResource(R.string.model),
            state.values["model"].orEmpty(),
            state.models.map {
                it to
                    it
            },
            onSelected = { onChange("model", it) },
            allowManual = true,
            enabled = !state.saving && !state.values["make"].isNullOrBlank()
        )
        if (state.values["make"].isNullOrBlank()) {
            Text(
                stringResource(R.string.choose_make_first),
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (state.catalogueLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(stringResource(R.string.catalogue_help), style = MaterialTheme.typography.bodySmall)
        if (state.catalogueLoading || !state.catalogueOnline) {
            val statusLabel =
                if (state.catalogueLoading) {
                    R.string.catalogue_loading
                } else {
                    R.string.catalogue_local
                }
            Text(
                stringResource(statusLabel),
                style = MaterialTheme.typography.bodySmall
            )
        }
        SearchChoiceField(
            stringResource(R.string.year),
            state.values["year"].orEmpty(),
            (LocalDate.now().year + 1 downTo 1886).map { it.toString() to it.toString() },
            onSelected = { onChange("year", it) },
            numeric = true
        )
        FuelChoice(state, onChange)
    } else {
        Text(
            listOfNotNull(
                state.values["make"],
                state.values["model"],
                state.values["year"]
            ).joinToString(" · "),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            stringResource(R.string.vehicle_reading_help),
            style = MaterialTheme.typography.bodyMedium
        )
        if (!state.isEdit) {
            DistanceChoice(state, onChange)
            FormField("odometer", R.string.odometer, state, onChange, numeric = true)
        }
        CurrencyChoice(state, onChange)
        OutlinedButton(onClick = onAttach, enabled = !state.saving) {
            Text(stringResource(R.string.photo))
        }
        if (!state.values["photo"].isNullOrBlank()) {
            VehiclePhoto(
                state.values.getValue("photo"),
                listOfNotNull(state.values["make"], state.values["model"]).joinToString(" ")
            )
            TextButton(onClick = {
                onChange("photo", "")
            }) { Text(stringResource(R.string.remove_photo)) }
        }
        OptionalSection(stringResource(R.string.optional_details)) {
            FormField("nickname", R.string.nickname, state, onChange)
            if (!state.isEdit) DateChoice("date", R.string.date, state, onChange)
            FormField("generation", R.string.generation, state, onChange)
            FormField("engine", R.string.engine, state, onChange)
            SearchChoiceField(
                stringResource(R.string.transmission),
                state.values["transmission"].orEmpty(),
                listOf(
                    "Automatic" to stringResource(R.string.transmission_auto),
                    "Manual" to stringResource(R.string.transmission_manual),
                    "CVT" to stringResource(R.string.transmission_cvt),
                    "Dual-clutch" to stringResource(R.string.transmission_dct),
                    "Robotized" to stringResource(R.string.transmission_robot)
                ),
                onSelected = { onChange("transmission", it) },
                allowManual = true
            )
            val locale = appLocale()
            val countries =
                remember(locale) {
                    Locale.getISOCountries().map { code ->
                        code to
                            Locale.Builder().setRegion(code).build().getDisplayCountry(locale)
                    }.sortedBy { it.second }
                }
            SearchChoiceField(
                stringResource(
                    R.string.market
                ),
                state.values["market"].orEmpty(),
                countries,
                onSelected = {
                    onChange("market", it)
                },
                allowManual = true
            )
            DateChoice("purchaseDate", R.string.purchase_date, state, onChange, optional = true)
            FormField("purchaseMileage", R.string.purchase_mileage, state, onChange, numeric = true)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlanForm(state: EditorUiState, onChange: (String, String) -> Unit) {
    Text(stringResource(R.string.plan_what), style = MaterialTheme.typography.titleLarge)
    CategoryTiles(state, onChange)
    FormField("title", R.string.title, state, onChange)
    Text(stringResource(R.string.plan_when), style = MaterialTheme.typography.titleLarge)
    val mode = state.values["dueMode"].orEmpty().ifBlank { "date" }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "date" to R.string.by_date,
            "distance" to R.string.by_distance,
            "both" to R.string.by_both
        ).forEach { (value, label) ->
            FilterChip(selected = mode == value, onClick = {
                onChange("dueMode", value)
            }, label = { Text(stringResource(label)) })
        }
    }
    if (mode != "distance") {
        DateChoice("dueDate", R.string.choose_due_date, state, onChange)
        val today = LocalDate.parse(state.values["today"] ?: LocalDate.now().toString())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                today.plusWeeks(1) to R.string.one_week,
                today.plusMonths(1) to R.string.one_month,
                today.plusMonths(3) to R.string.three_months
            ).forEach { (date, label) ->
                SuggestionChip(onClick = {
                    onChange("dueDate", date.toString())
                }, label = { Text(stringResource(label)) })
            }
        }
    }
    if (mode != "date") {
        DistanceChoice(state, onChange)
        FormField("dueKm", R.string.plan_due_mileage, state, onChange, numeric = true)
        state.values["odometerBaseline"]?.takeIf { it.isNotBlank() }?.let { reading ->
            Text(
                stringResource(R.string.plan_last_reading, "$reading ${unitLabel(state)}"),
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("1000", "5000", "10000", "15000").forEach { amount ->
                    SuggestionChip(onClick = {
                        onChange("dueAfter", amount)
                    }, label = {
                        Text(stringResource(R.string.additional_distance, amount, unitLabel(state)))
                    })
                }
            }
        }
    }
    Toggle("repeatPlan", R.string.repeat_plan, state, onChange)
    if (state.values["repeatPlan"].toBoolean()) {
        if (mode !=
            "distance"
        ) {
            NumberChoice(
                "intervalMonths",
                R.string.repeat_months,
                state,
                onChange,
                listOf(1, 3, 6, 12, 18, 24, 36, 60)
            )
        }
        if (mode !=
            "date"
        ) {
            NumberChoice(
                "intervalKm",
                R.string.repeat_distance,
                state,
                onChange,
                listOf(1000, 5000, 7500, 10000, 15000, 20000)
            )
        }
        Text(stringResource(R.string.repeat_help), style = MaterialTheme.typography.bodySmall)
    }
    Toggle("reminders", R.string.reminders, state, onChange)
    Text(stringResource(R.string.plan_simple_help), style = MaterialTheme.typography.bodySmall)
    OptionalSection(stringResource(R.string.plan_extra)) {
        NumberChoice(
            "warningDays",
            R.string.warning_days,
            state,
            onChange,
            listOf(0, 1, 3, 7, 14, 30)
        )
        if (mode !=
            "date"
        ) {
            NumberChoice(
                "warningKm",
                R.string.warning_distance,
                state,
                onChange,
                listOf(0, 100, 500, 1000)
            )
        }
        FormField("estimated", R.string.estimated_cost, state, onChange, numeric = true)
        CurrencyChoice(state, onChange)
        FormField("notes", R.string.notes, state, onChange, multiline = true)
        Text(stringResource(R.string.reminder_help), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun FuelForm(
    state: EditorUiState,
    onChange: (String, String) -> Unit,
    onAttach: () -> Unit
) {
    Text(stringResource(R.string.fuel_simple_help))
    FormField("volume", R.string.volume, state, onChange, numeric = true)
    FormField("amount", R.string.total_paid, state, onChange, numeric = true)
    Text(
        "${state.values["currency"].orEmpty()} · ${volumeUnitLabel(state)}",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Toggle("fullTank", R.string.full_tank, state, onChange)
    OptionalSection(stringResource(R.string.fuel_tank_explain)) {
        Text(stringResource(R.string.full_tank_help))
    }
    if (!state.values["odometerBaseline"].isNullOrBlank()) {
        Text(
            stringResource(
                R.string.plan_last_reading,
                "${state.values["odometerBaseline"]} ${unitLabel(state)}"
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
    Toggle("recordMileage", R.string.fuel_record_mileage, state, onChange)
    if (state.values["recordMileage"].toBoolean()) {
        DistanceChoice(state, onChange)
        FormField("odometer", R.string.odometer, state, onChange, numeric = true)
    }
    Toggle("estimateMileage", R.string.estimate_from_consumption, state, onChange)
    if (state.values["estimateMileage"].toBoolean()) {
        FormField(
            "estimatedConsumption",
            R.string.assumed_consumption,
            state,
            onChange,
            numeric = true
        )
        Text(
            stringResource(R.string.estimate_consumption_help),
            style = MaterialTheme.typography.bodySmall
        )
        state.estimatedFuelDistanceKm?.let { km ->
            Text(
                stringResource(
                    R.string.estimated_fuel_distance,
                    distance(km, state.values["distanceUnit"].orEmpty())
                ),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            stringResource(R.string.estimated_mileage_warning),
            style = MaterialTheme.typography.bodySmall
        )
    }
    Text(stringResource(R.string.fuel_mileage_later), style = MaterialTheme.typography.bodySmall)
    OptionalSection(stringResource(R.string.fuel_more)) {
        DateChoice("date", R.string.date, state, onChange)
        TimeChoice(state, onChange)
        FuelChoice(state, onChange)
        VolumeChoice(state, onChange)
        CurrencyChoice(state, onChange)
        FormField("unitPrice", R.string.unit_price, state, onChange, numeric = true)
        Text(stringResource(R.string.fuel_confirmation), style = MaterialTheme.typography.bodySmall)
        Toggle("missingFill", R.string.missing_fill, state, onChange)
        FormField("station", R.string.station, state, onChange)
        AttachmentControls(state, onChange, onAttach)
        FormField("notes", R.string.notes, state, onChange, multiline = true)
        Text(
            stringResource(R.string.fuel_no_estimated_mileage),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
internal fun JournalRecordForm(
    mode: String,
    state: EditorUiState,
    onChange: (String, String) -> Unit,
    onAttach: () -> Unit
) {
    DateChoice("date", R.string.date, state, onChange)
    if (mode == "service") {
        CategoryTiles(state, onChange)
        FormField("title", R.string.title, state, onChange)
        FormField("odometer", R.string.odometer, state, onChange, numeric = true)
        FormField("amount", R.string.total_paid, state, onChange, numeric = true)
    } else if (mode == "expense") {
        CategoryChoice(state, onChange)
        FormField("title", R.string.title, state, onChange)
        FormField("amount", R.string.total_paid, state, onChange, numeric = true)
    } else if (mode == "odometer") {
        DistanceChoice(state, onChange)
        FormField("odometer", R.string.odometer, state, onChange, numeric = true)
    } else {
        FormField("title", R.string.title, state, onChange)
        FormField("notes", R.string.notes, state, onChange, multiline = true)
    }
    if (mode !=
        "note"
    ) {
        OptionalSection(
            stringResource(
                if (mode ==
                    "service"
                ) {
                    R.string.service_more
                } else {
                    R.string.optional_details
                }
            )
        ) {
            if (mode != "service") TimeChoice(state, onChange)
            if (mode in listOf("service", "expense")) {
                CurrencyChoice(state, onChange)
                DistanceChoice(state, onChange)
                if (mode ==
                    "expense"
                ) {
                    FormField("odometer", R.string.odometer, state, onChange, numeric = true)
                }
                FormField("station", R.string.station, state, onChange)
                AttachmentControls(state, onChange, onAttach)
            }
            FormField("notes", R.string.notes, state, onChange, multiline = true)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryTiles(state: EditorUiState, onChange: (String, String) -> Unit) {
    val previousTitle =
        stringResource(
            categoryLabel(
                Category.valueOf(state.values["category"] ?: "ENGINE_OIL")
            )
        )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Category.entries.take(8).forEach { category ->
            val title = stringResource(categoryLabel(category))
            FilterChip(selected = state.values["category"] == category.name, onClick = {
                onChange("category", category.name)
                if (state.values["title"].isNullOrBlank() ||
                    state.values["title"] == previousTitle
                ) {
                    onChange("title", title)
                }
            }, label = { Text(title) })
        }
    }
    CategoryChoice(state, onChange)
}

@Composable
internal fun CurrencyChoice(state: EditorUiState, onChange: (String, String) -> Unit) {
    val locale = appLocale()
    val choices =
        remember(locale) {
            val currencies =
                (
                    listOf("UAH", "EUR", "USD", "PLN", "GBP") +
                        Currency.getAvailableCurrencies().map { it.currencyCode }.sorted()
                    ).distinct()
            currencies.filter { Currency.getInstance(it).defaultFractionDigits >= 0 }.map {
                it to
                    "$it · ${Currency.getInstance(it).getDisplayName(locale)}"
            }
        }
    SearchChoiceField(
        stringResource(
            R.string.currency
        ),
        state.values["currency"].orEmpty(),
        choices,
        onSelected = {
            onChange("currency", it)
        }
    )
}

@Composable
private fun NumberChoice(
    key: String,
    label: Int,
    state: EditorUiState,
    onChange: (String, String) -> Unit,
    numbers: List<Int>
) {
    SearchChoiceField(
        stringResource(label),
        state.values[key].orEmpty(),
        numbers.map {
            it.toString() to
                it.toString()
        },
        onSelected = { onChange(key, it) },
        allowManual = true,
        numeric = true
    )
}

@Composable
private fun VolumeChoice(state: EditorUiState, onChange: (String, String) -> Unit) {
    ChoiceField(
        stringResource(R.string.volume_unit),
        state.values["volumeUnit"].orEmpty(),
        listOf(
            "L" to stringResource(R.string.liters),
            "US gal" to stringResource(R.string.us_gallons)
        )
    ) { onChange("volumeUnit", it) }
}

@Composable
private fun unitLabel(state: EditorUiState): String = stringResource(
    if (state.values["distanceUnit"] ==
        "mi"
    ) {
        R.string.mi
    } else {
        R.string.km
    }
)

@Composable
private fun volumeUnitLabel(state: EditorUiState): String = stringResource(
    if (state.values["volumeUnit"] ==
        "US gal"
    ) {
        R.string.us_gallons
    } else {
        R.string.liters
    }
)

@Composable
private fun AttachmentControls(
    state: EditorUiState,
    onChange: (String, String) -> Unit,
    onAttach: () -> Unit
) {
    OutlinedButton(onClick = onAttach, enabled = !state.saving) {
        Text(stringResource(R.string.attachment))
    }
    state.values["attachments"].orEmpty().split('|').filter {
        it.isNotBlank()
    }.forEachIndexed { index, attachment ->
        TextButton(onClick = {
            onChange(
                "attachments",
                state.values["attachments"].orEmpty().split('|').filter {
                    it !=
                        attachment
                }.joinToString("|")
            )
        }) {
            Text(stringResource(R.string.remove_attachment, index + 1))
        }
    }
}
