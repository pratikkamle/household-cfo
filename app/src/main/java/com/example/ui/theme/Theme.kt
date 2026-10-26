package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldGreen,
    onPrimary = NavyDark,
    primaryContainer = NavyLight,
    onPrimaryContainer = EmeraldLight,
    secondary = CyanAccent,
    onSecondary = NavyDark,
    secondaryContainer = NavyCard,
    onSecondaryContainer = CyanAccent,
    tertiary = GoldAccent,
    background = NavyDark,
    onBackground = TextPrimary,
    surface = NavyCard,
    onSurface = TextPrimary,
    surfaceVariant = NavySurface,
    onSurfaceVariant = TextSecondary,
    error = CoralRed,
    outline = NavyLight
)

@Composable
fun HouseholdCFOTheme(
    darkTheme: Boolean = true, // Executive dark CFO theme as primary
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = NavyDark.toArgb()
                window.navigationBarColor = NavyDark.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
