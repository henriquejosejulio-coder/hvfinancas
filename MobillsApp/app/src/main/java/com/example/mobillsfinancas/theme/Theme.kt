package com.example.mobillsfinancas.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MobillsGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = MobillsGreenContainer,
    onPrimaryContainer = MobillsGreenLight,
    secondary = ColorInter,
    background = NeutralDarkBackground,
    surface = NeutralDarkSurface,
    surfaceVariant = NeutralDarkCard,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    error = ExpenseRed
)

private val LightColorScheme = lightColorScheme(
    primary = MobillsGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = MobillsGreenLight,
    onPrimaryContainer = MobillsGreenDark,
    secondary = ColorInter,
    background = NeutralLightBackground,
    surface = NeutralLightSurface,
    surfaceVariant = NeutralLightCard,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    error = ExpenseRed
)

@Composable
fun MobillsFinancasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
