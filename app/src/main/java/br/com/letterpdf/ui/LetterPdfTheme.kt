package br.com.letterpdf.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val LightColors = lightColorScheme(
    primary = Color(0xFF17694D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDEFE7),
    onPrimaryContainer = Color(0xFF0A3324),
    secondary = Color(0xFF4E6359),
    background = Color(0xFFF7F7F2),
    onBackground = Color(0xFF17211D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17211D),
    surfaceVariant = Color(0xFFE8ECE6),
    onSurfaceVariant = Color(0xFF414944),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DD8B6),
    onPrimary = Color(0xFF003823),
    primaryContainer = Color(0xFF075236),
    onPrimaryContainer = Color(0xFFB8F5D4),
    secondary = Color(0xFFB4CCBD),
    background = Color(0xFF101512),
    onBackground = Color(0xFFE0E6DF),
    surface = Color(0xFF171D19),
    onSurface = Color(0xFFE0E6DF),
    surfaceVariant = Color(0xFF29332D),
    onSurfaceVariant = Color(0xFFC0CAC2),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun LetterPdfTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = colors, content = content)
}
