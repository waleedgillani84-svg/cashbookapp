package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CashBlue,
    onPrimary = Color.White,
    primaryContainer = CashBlueContainer,
    onPrimaryContainer = CashBlue,
    secondary = CashGreen,
    onSecondary = Color.White,
    secondaryContainer = CashGreenContainer,
    onSecondaryContainer = CashGreen,
    tertiary = CashRed,
    onTertiary = Color.White,
    tertiaryContainer = CashRedContainer,
    onTertiaryContainer = CashRed,
    background = CashBookBg,
    onBackground = TextPrimary,
    surface = CashBookSurface,
    onSurface = TextPrimary,
    surfaceVariant = CashBookSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CashBookBorder
)

private val LightColorScheme = lightColorScheme(
    primary = CashBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E40AF),
    secondary = CashGreenDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCFCE7),
    onSecondaryContainer = Color(0xFF166534),
    tertiary = CashRedDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEE2E2),
    onTertiaryContainer = Color(0xFF991B1B),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek financial dark mode
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
