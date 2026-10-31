package com.example.drivecare.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF2563EB),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE0EBFF),
        onPrimaryContainer = Color(0xFF123C85),
        secondary = Color(0xFF51647F),
        secondaryContainer = Color(0xFFE8EDF5),
        background = Color(0xFFF6F7FB),
        onBackground = Color(0xFF172033),
        surface = Color.White,
        onSurface = Color(0xFF172033),
        surfaceVariant = Color(0xFFECF0F6),
        onSurfaceVariant = Color(0xFF536176),
        error = Color(0xFFB42318),
        outline = Color(0xFF758095)
    )
private val DarkColors =
    darkColorScheme(
        primary = Color(0xFFAEC7FF),
        onPrimary = Color(0xFF00316C),
        primaryContainer = Color(0xFF174A94),
        onPrimaryContainer = Color(0xFFD8E4FF),
        secondary = Color(0xFFB7C7DF),
        secondaryContainer = Color(0xFF2F415A),
        background = Color(0xFF101722),
        onBackground = Color(0xFFE3E9F3),
        surface = Color(0xFF192331),
        onSurface = Color(0xFFE3E9F3),
        surfaceVariant = Color(0xFF293548),
        onSurfaceVariant = Color(0xFFBECADB),
        error = Color(0xFFFFB4A9),
        outline = Color(0xFF8D9AB0)
    )

@Composable
fun DriveCareTheme(theme: String = "system", content: @Composable () -> Unit) {
    val dark =
        when (theme) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme()
        }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            view.context.findActivity()?.let { activity ->
                WindowCompat.getInsetsController(activity.window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
