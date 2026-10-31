package com.example.drivecare.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.drivecare.R
import com.example.drivecare.core.model.*
import com.example.drivecare.domain.*
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

fun fuelLabel(type: FuelType): Int = when (type) {
    FuelType.PETROL -> R.string.petrol
    FuelType.DIESEL -> R.string.diesel
    FuelType.LPG -> R.string.lpg
    FuelType.PETROL_HYBRID -> R.string.petrol_hybrid
    FuelType.DIESEL_HYBRID -> R.string.diesel_hybrid
    FuelType.PLUG_IN_HYBRID -> R.string.plug_in_hybrid
}

fun categoryLabel(category: Category): Int = when (category) {
    Category.ENGINE_OIL -> R.string.engine_oil
    Category.OIL_FILTER -> R.string.oil_filter
    Category.AIR_FILTER -> R.string.air_filter
    Category.CABIN_FILTER -> R.string.cabin_filter
    Category.TIRES -> R.string.tires
    Category.BRAKES -> R.string.brakes
    Category.COOLANT -> R.string.coolant
    Category.INSPECTION -> R.string.inspection
    Category.REPAIR -> R.string.repair
    Category.PARKING -> R.string.parking
    Category.WASHING -> R.string.washing
    Category.INSURANCE -> R.string.insurance
    Category.REGISTRATION -> R.string.registration
    Category.ACCESSORIES -> R.string.accessories
    Category.OTHER -> R.string.other
}

fun statusLabel(status: DueStatus): Int = when (status) {
    DueStatus.PLANNED -> R.string.planned
    DueStatus.DUE_SOON -> R.string.due_soon
    DueStatus.DUE -> R.string.due
    DueStatus.OVERDUE -> R.string.overdue
    DueStatus.PAUSED -> R.string.paused
    DueStatus.COMPLETED -> R.string.completed
}

fun money(amount: Long, currency: String, locale: Locale): String =
    NumberFormat.getCurrencyInstance(locale)
        .apply {
            this.currency = Currency.getInstance(currency)
            maximumFractionDigits = Money.scale(currency)
            minimumFractionDigits = Money.scale(currency)
        }
        .format(Money.major(amount, currency))

fun number(value: BigDecimal, locale: Locale, digits: Int = 2): String =
    NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = digits
    }.format(value)

fun date(value: String, locale: Locale): String = LocalDate.parse(value)
    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

@Composable fun appLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable
fun distance(km: String, unit: String): String = stringResource(
    R.string.value_unit,
    number(Units.fromKm(km.decimal(), unit), appLocale(), 0),
    stringResource(if (unit == "mi") R.string.mi else R.string.km)
)

@Composable
fun volume(liters: BigDecimal, unit: String): String = stringResource(
    R.string.value_unit,
    number(Units.fromLiters(liters, unit), appLocale()),
    stringResource(if (unit == "US gal") R.string.us_gallons else R.string.liters)
)

@Composable
fun PageColumn(artwork: Artwork? = null, content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = 1100.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
                    .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (artwork != null) ScreenArtwork(artwork)
            content()
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit = {
    }
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            content()
        }
    }
}

@Composable
fun EmptyVehicle(onAdd: () -> Unit, artwork: Artwork? = Artwork.GARAGE) {
    PageColumn(artwork) {
        InfoCard(stringResource(R.string.no_vehicle), stringResource(R.string.no_vehicle_help)) {
            Button(onClick = onAdd) {
                Text(stringResource(R.string.add_vehicle))
            }
        }
    }
}

@Composable
fun Message(@StringRes message: Int?, onDismiss: () -> Unit) {
    if (message != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            text = {
                Text(stringResource(message))
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.continue_action))
                }
            }
        )
    }
}

@Composable
fun DeleteConfirmation(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.confirm_delete))
        },
        text = {
            Text(stringResource(R.string.delete_warning))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoiceField(
    label: String,
    selected: String,
    choices: List<Pair<String, String>>,
    onSelected: (String) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = {
        expanded = it
    }) {
        OutlinedTextField(
            value =
                choices.find {
                    it.first == selected
                }?.second ?: selected,
            onValueChange = {
            },
            readOnly = true,
            label = {
                Text(label)
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = {
            expanded = false
        }) {
            choices.forEach { (id, title) ->
                DropdownMenuItem(
                    text = {
                        Text(title)
                    },
                    onClick = {
                        expanded = false
                        onSelected(id)
                    }
                )
            }
        }
    }
}

@Composable
fun consumption(value: BigDecimal, distanceUnit: String, volumeUnit: String): String {
    val result =
        when {
            distanceUnit == "mi" && volumeUnit == "US gal" -> {
                if (value.signum() == 0) return stringResource(R.string.not_enough_data)
                BigDecimal("235.214583").divide(value, 6, java.math.RoundingMode.HALF_UP) to
                    R.string.consumption_mpg
            }

            distanceUnit == "mi" ->
                value.multiply(BigDecimal("1.609344")) to R.string.consumption_l100mi

            volumeUnit == "US gal" ->
                value.divide(BigDecimal("3.785411784"), 6, java.math.RoundingMode.HALF_UP) to
                    R.string.consumption_gal100km

            else -> value to R.string.consumption_value
        }
    return stringResource(result.second, number(result.first, appLocale()))
}
