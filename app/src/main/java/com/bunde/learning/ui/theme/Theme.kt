package com.bunde.learning.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BundeAccent,
    onPrimary = BundeSurface,
    primaryContainer = BundeAccentLight,
    onPrimaryContainer = BundeAccentDark,
    secondary = BundeHighlight,
    onSecondary = BundeSurface,
    background = BundeBackground,
    onBackground = BundeTextPrimary,
    surface = BundeSurface,
    onSurface = BundeTextPrimary,
    surfaceVariant = BundeBackground,
    onSurfaceVariant = BundeTextSecondary,
    outline = BundeBorder
)

@Composable
fun BundeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // For Bunde's signature calm, warm paper visual identity (#F2EEE4), we default to the light warm palette
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = BundeBackground.toArgb()
            window.navigationBarColor = BundeBackground.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = true
            insetsController.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BundeTypography,
        content = content
    )
}
