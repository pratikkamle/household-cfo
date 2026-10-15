package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val DarkColorScheme = darkColorScheme(
    primary = Emerald400,
    onPrimary = Slate950,
    primaryContainer = Emerald900,
    onPrimaryContainer = Emerald100,
    secondary = OceanTeal,
    onSecondary = Color.White,
    secondaryContainer = Slate800,
    onSecondaryContainer = Slate200,
    tertiary = RosePink,
    onTertiary = Color.White,
    tertiaryContainer = Slate800,
    onTertiaryContainer = RosePinkLight,
    background = Slate950,
    onBackground = Slate50,
    surface = Slate900,
    onSurface = Slate50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    outlineVariant = Slate800
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald700,
    onPrimary = Color.White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald900,
    secondary = OceanTeal,
    onSecondary = Color.White,
    secondaryContainer = OceanTealLight,
    onSecondaryContainer = Slate900,
    tertiary = RosePink,
    onTertiary = Color.White,
    tertiaryContainer = RosePinkLight,
    onTertiaryContainer = Slate900,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate200,
    outlineVariant = Slate100
)

@Composable
fun HouseholdCfoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep distinct custom financial branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Oppo/ColorOS devices often apply aggressive display scaling / font scaling (>1.15x)
    // which causes financial dashboard metrics and navigation labels to look bulky and wrap letters onto new lines.
    // Clamping the fontScale gently to 1.05f ensures a sleek, clean, compact aesthetic (matching Nothing OS)
    // while preserving comfortable legibility and accessibility.
    val currentDensity = LocalDensity.current
    val clampedDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale.coerceIn(0.85f, 1.0f)
    )

    CompositionLocalProvider(LocalDensity provides clampedDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
