package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val RelayDarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = CyanOnPrimary,
    primaryContainer = CyanContainer,
    onPrimaryContainer = CyanOnContainer,
    secondary = EmeraldSecondary,
    onSecondary = EmeraldOnSecondary,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = EmeraldOnContainer,
    tertiary = AmberTertiary,
    onTertiary = AmberOnTertiary,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberOnContainer,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    error = CrimsonError,
    onError = Color.White,
    errorContainer = CrimsonContainerDark,
    onErrorContainer = Color(0xFFFECACA)
)

private val RelayLightColorScheme = lightColorScheme(
    primary = SkyPrimaryLight,
    onPrimary = SkyOnPrimaryLight,
    primaryContainer = SkyContainerLight,
    onPrimaryContainer = SkyOnContainerLight,
    secondary = EmeraldSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainerLight,
    onSecondaryContainer = EmeraldOnContainerLight,
    tertiary = AmberTertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = AmberOnContainerLight,
    background = CloudBg,
    onBackground = TextPrimaryLight,
    surface = CloudSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = CloudSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = CloudSurface,
    surfaceContainerLow = CloudBg,
    surfaceContainer = CloudSurfaceElevated,
    outline = CloudOutline,
    error = CrimsonError,
    onError = Color.White
)

@Composable
fun RelayWebTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> RelayDarkColorScheme
        else -> RelayLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RelayWebTypography,
        content = content
    )
}
