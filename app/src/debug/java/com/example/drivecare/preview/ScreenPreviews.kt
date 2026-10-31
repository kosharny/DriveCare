package com.example.drivecare.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.drivecare.core.model.*
import com.example.drivecare.core.ui.FeatureUiState
import com.example.drivecare.feature.editor.EditorScreen
import com.example.drivecare.feature.editor.EditorUiState
import com.example.drivecare.feature.overview.OverviewScreen
import com.example.drivecare.feature.statistics.StatisticsScreen
import com.example.drivecare.ui.theme.DriveCareTheme

private val previewVehicle =
    Vehicle(
        make = "Toyota",
        model = "Corolla",
        year = 2020,
        fuelType = FuelType.PETROL,
        currency = "UAH",
        nickname = "My car"
    )
private val previewState =
    FeatureUiState(
        snapshot = StoreSnapshot(vehicles = listOf(previewVehicle), loaded = true),
        prefs = AppPreferences(loaded = true, onboardingDone = true)
    )

@Preview(name = "Empty garage", showBackground = true)
@Composable
private fun EmptyOverview() {
    DriveCareTheme {
        OverviewScreen(FeatureUiState(), {
        }, {
        }, {
        }, {
        })
    }
}

@Preview(name = "Overview compact", showBackground = true, widthDp = 412, heightDp = 900)
@Preview(name = "Overview expanded", showBackground = true, widthDp = 1100, heightDp = 800)
@Preview(name = "Overview Ukrainian", locale = "uk", showBackground = true)
@Composable
private fun Overview() {
    DriveCareTheme {
        OverviewScreen(previewState, {
        }, {
        }, {
        }, {
        })
    }
}

@Preview(name = "Fuel form Russian", locale = "ru", showBackground = true, heightDp = 1000)
@Composable
private fun FuelForm() {
    DriveCareTheme {
        EditorScreen(
            "fuel",
            EditorUiState(
                loaded = true,
                values =
                    mapOf(
                        "distanceUnit" to "km",
                        "volumeUnit" to "L",
                        "fuelType" to "PETROL",
                        "currency" to "UAH"
                    )
            ),
            { _, _ ->
            },
            {
            },
            {
            },
            {
            }
        )
    }
}

@Preview(name = "Statistics dark", showBackground = true, widthDp = 800, heightDp = 900)
@Composable
private fun StatisticsEmpty() {
    DriveCareTheme("dark") {
        StatisticsScreen(
            previewState,
            previewState.monthStatistics,
            null,
            "month",
            "",
            "",
            "",
            { _, _ ->
            },
            {
            },
            {
            }
        )
    }
}
