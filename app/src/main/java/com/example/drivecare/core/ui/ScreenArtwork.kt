package com.example.drivecare.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.drivecare.R

enum class Artwork(val fileName: String, val fallback: Int) {
    OVERVIEW("overview", R.drawable.art_overview),
    MAINTENANCE("maintenance", R.drawable.art_maintenance),
    JOURNAL("journal", R.drawable.art_journal),
    STATISTICS("statistics", R.drawable.art_statistics),
    GARAGE("garage", R.drawable.art_garage),
    VEHICLE("vehicle_setup", R.drawable.art_vehicle_setup),
    PLAN("maintenance_setup", R.drawable.art_maintenance_setup),
    FUEL("fuel_entry", R.drawable.art_fuel_entry),
    EXPENSE("expense_entry", R.drawable.art_expense_entry),
    SERVICE("service_entry", R.drawable.art_service_entry),
    ODOMETER("odometer_entry", R.drawable.art_odometer_entry),
    NOTE("note_entry", R.drawable.art_note_entry),
    SETTINGS("settings", R.drawable.art_settings),
    PRO("pro", R.drawable.art_pro),
    ONBOARD_CARE("onboarding_maintenance", R.drawable.art_onboarding_maintenance),
    ONBOARD_COSTS("onboarding_costs", R.drawable.art_onboarding_costs),
    ONBOARD_AI("onboarding_ai", R.drawable.art_onboarding_ai)
}

@Composable
fun ScreenArtwork(artwork: Artwork, onboarding: Boolean = false) {
    val windowSize = LocalWindowInfo.current.containerSize
    val windowHeight = with(LocalDensity.current) { windowSize.height.toDp() }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val ratio = if (onboarding) 1.6f else 2.5f
        val maximumHeight = if (onboarding) 300.dp else 220.dp
        val artworkHeight = minOf(maxWidth / ratio, maximumHeight, windowHeight * 0.26f)
        Surface(
            modifier = Modifier.fillMaxWidth().height(artworkHeight),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            AsyncImage(
                model = "file:///android_asset/artwork/${artwork.fileName}.webp",
                contentDescription = null,
                placeholder = painterResource(artwork.fallback),
                error = painterResource(artwork.fallback),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
