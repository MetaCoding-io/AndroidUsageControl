package com.usagecontrol.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

/**
 * Doorman theme: dark-first ink/paper palette, Manrope + DM Mono type, expressive motion.
 * [dynamicColor] (Android 12+) only recolours the accent roles; grounds, surfaces
 * and lane colours always stay Doorman's.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DoormanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) DoormanDarkColors else DoormanLightColors
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        val dynamic = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        base.withAccentFrom(dynamic)
    } else {
        base
    }

    CompositionLocalProvider(LocalLaneColors provides laneColors(darkTheme)) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = DoormanShapes,
            typography = DoormanTypography,
            content = content,
        )
    }
}

private fun ColorScheme.withAccentFrom(dynamic: ColorScheme): ColorScheme = copy(
    primary = dynamic.primary,
    onPrimary = dynamic.onPrimary,
    primaryContainer = dynamic.primaryContainer,
    onPrimaryContainer = dynamic.onPrimaryContainer,
    inversePrimary = dynamic.inversePrimary,
)
