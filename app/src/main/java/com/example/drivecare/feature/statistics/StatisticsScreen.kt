package com.example.drivecare.feature.statistics

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import java.math.BigDecimal

@Composable
fun StatisticsRoute(onAddVehicle: () -> Unit, onPro: () -> Unit) {
    val vm: StatisticsViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    val reportCurrency by vm.reportCurrency.collectAsStateWithLifecycle()
    val advanced by vm.advanced.collectAsStateWithLifecycle()
    val period by vm.period.collectAsStateWithLifecycle()
    val start by vm.start.collectAsStateWithLifecycle()
    val end by vm.end.collectAsStateWithLifecycle()
    val pdf =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/pdf")
        ) { uri ->
            if (uri != null) vm.exportPdf(uri)
        }
    if (state.vehicle == null) {
        EmptyVehicle(onAddVehicle, Artwork.STATISTICS)
        return
    }
    StatisticsScreen(
        state = state,
        result = result,
        advanced = advanced,
        period = period,
        start = start,
        end = end,
        reportCurrency = reportCurrency,
        onFilter = vm::filter,
        onPro = onPro
    ) {
        if (state.billing.pro) pdf.launch("DriveCare-report.pdf") else onPro()
    }
    Message(state.error, vm::acknowledge)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatisticsScreen(
    state: FeatureUiState,
    result: Statistics?,
    advanced: AdvancedStatistics?,
    period: String,
    start: String,
    end: String,
    reportCurrency: String,
    onFilter: (String, String) -> Unit,
    onPro: () -> Unit,
    onPdf: () -> Unit
) {
    val locale = appLocale()
    PageColumn(Artwork.STATISTICS) {
        ChoiceField(
            stringResource(R.string.period),
            period,
            listOf(
                "month" to stringResource(R.string.this_month),
                "previous" to stringResource(R.string.previous_month),
                "year" to stringResource(R.string.this_year),
                "all" to stringResource(R.string.all_time),
                "custom" to stringResource(R.string.custom_period)
            )
        ) {
            onFilter("period", it)
        }
        if (period == "custom") {
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
        }
        if (result == null) {
            Text(stringResource(R.string.invalid_fields), color = MaterialTheme.colorScheme.error)
        } else {
            if (result.records.isEmpty()) Text(stringResource(R.string.no_data))
            BoxWithConstraints {
                val columns = if (maxWidth >= 700.dp) 2 else 1
                FlowRow(
                    maxItemsInEachRow = columns,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    result.totals.forEach { total ->
                        Column(Modifier.weight(1f)) {
                            InfoCard(stringResource(R.string.ownership_total), total.currency) {
                                Text(
                                    money(total.totalMinor, total.currency, locale),
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Text(
                                    stringResource(R.string.fuel_spending) +
                                        ": " +
                                        money(total.fuelMinor, total.currency, locale)
                                )
                                Text(
                                    stringResource(R.string.service) +
                                        ": " +
                                        money(total.serviceMinor, total.currency, locale)
                                )
                                Text(
                                    stringResource(R.string.expense) +
                                        ": " +
                                        money(total.otherMinor, total.currency, locale)
                                )
                            }
                        }
                    }
                    result.fuelGroups.forEach { group ->
                        Column(Modifier.weight(1f)) {
                            InfoCard(stringResource(fuelLabel(group.fuelType)), group.currency) {
                                Text(stringResource(R.string.records_count, group.count))
                                Text(
                                    stringResource(R.string.purchased_volume) +
                                        ": " +
                                        volume(group.liters, state.prefs.volumeUnit)
                                )
                                group.pricePerLiter?.let { price ->
                                    val inputPrice =
                                        if (state.prefs.volumeUnit == "US gal") {
                                            price.multiply(BigDecimal("3.785411784"))
                                        } else {
                                            price
                                        }
                                    Text(stringResource(R.string.weighted_price))
                                    Text(
                                        stringResource(
                                            R.string.price_value,
                                            "${number(inputPrice, locale, 3)} ${group.currency}",
                                            stringResource(
                                                if (state.prefs.volumeUnit == "US gal") {
                                                    R.string.us_gallons
                                                } else {
                                                    R.string.liters
                                                }
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            InfoCard(stringResource(R.string.fuel_consumption)) {
                val consumption = result.consumption.litersPer100Km
                Text(
                    if (consumption == null) {
                        stringResource(R.string.not_enough_data)
                    } else {
                        com.example.drivecare.core.ui.consumption(
                            consumption,
                            state.prefs.distanceUnit,
                            state.prefs.volumeUnit
                        )
                    },
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    stringResource(
                        if (result.consumption.unsupportedDualFuel) {
                            R.string.dual_fuel_help
                        } else {
                            R.string.consumption_method
                        }
                    )
                )
                if (result.consumption.excludedIntervals > 0) {
                    Text(
                        stringResource(
                            R.string.excluded_count,
                            result.consumption.excludedIntervals
                        )
                    )
                }
                if (state.vehicle?.fuelType == FuelType.PLUG_IN_HYBRID) {
                    Text(stringResource(R.string.phev_help))
                }
            }
            result.totals.forEach { total ->
                FuelChart(
                    result.monthlyFuel.mapValues {
                        it.value[total.currency] ?: 0
                    },
                    total.currency
                )
            }
            Text(
                stringResource(R.string.planned_cost_help),
                style = MaterialTheme.typography.bodySmall
            )
            if (state.billing.pro) {
                advanced?.let {
                    AdvancedReport(it)
                }
            } else {
                OutlinedButton(onClick = onPro) {
                    Text(stringResource(R.string.advanced_reports) + " · Pro")
                }
            }
            if (state.billing.pro) {
                val currencies =
                    (
                        result.totals.map { it.currency } +
                            listOfNotNull(state.vehicle?.currency)
                        ).distinct()
                ChoiceField(
                    stringResource(R.string.currency),
                    reportCurrency.ifBlank {
                        state.vehicle?.currency.orEmpty()
                    },
                    currencies.map { it to it }
                ) {
                    onFilter("reportCurrency", it)
                }
            }
            OutlinedButton(onClick = onPdf) {
                Text(stringResource(R.string.export_pdf))
            }
        }
    }
}

@Composable
private fun FuelChart(values: Map<String, Long>, currency: String) {
    val locale = appLocale()
    val color = MaterialTheme.colorScheme.primary
    var selected by rememberSaveable(currency) {
        mutableStateOf("")
    }
    val display = values.entries.toList()
    val description =
        display.joinToString("; ") {
            "${date(it.key, locale)}: ${money(it.value, currency, locale)}"
        }
    InfoCard(stringResource(R.string.monthly_chart), currency) {
        if (display.isNotEmpty()) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp).semantics {
                        contentDescription = description
                    }
            ) {
                val maximum =
                    display.maxOf {
                        it.value
                    }.coerceAtLeast(1).toFloat()
                val width = size.width / display.size
                display.forEachIndexed { index, item ->
                    val height = (item.value.toFloat() / maximum) * size.height
                    drawRect(
                        color,
                        topLeft = Offset(index * width + width * .15f, size.height - height),
                        size = Size(width * .7f, height)
                    )
                }
            }
            display.forEach { item ->
                TextButton(onClick = {
                    selected = item.key
                }) {
                    Text(date(item.key, locale) + " · " + money(item.value, currency, locale))
                }
            }
            values[selected]?.let {
                Text(
                    date(selected, locale) + ": " + money(it, currency, locale),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            Text(stringResource(R.string.no_data))
        }
    }
}

@Composable
private fun AdvancedReport(advanced: AdvancedStatistics) {
    val locale = appLocale()
    InfoCard(stringResource(R.string.advanced_reports)) {
        advanced.previousRange?.let {
            Text(
                stringResource(
                    R.string.report_period,
                    it.start.toString(),
                    it.endInclusive.toString()
                )
            )
        }
        advanced.previousTotals.forEach {
            Text(money(it.totalMinor, it.currency, locale))
        }
        advanced.categories.forEach {
            Text(
                stringResource(categoryLabel(it.category)) +
                    ": " +
                    money(it.minor, it.currency, locale)
            )
        }
    }
    InfoCard(stringResource(R.string.workshop_breakdown)) {
        advanced.places.forEach {
            Text(it.name + ": " + money(it.minor, it.currency, locale))
        }
    }
    InfoCard(stringResource(R.string.multi_vehicle)) {
        advanced.vehicles.forEach {
            Text(it.name + ": " + money(it.minor, it.currency, locale))
        }
    }
}
