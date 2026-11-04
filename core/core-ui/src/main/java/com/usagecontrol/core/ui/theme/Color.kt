package com.usagecontrol.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Doorman palette (R010 / D013). Dark-first: a cool blue-black "ink" ground with
 * four stepped surface containers, a warm "paper" light theme, and a single amber
 * accent (the LED dot). Ink 0xFF0E1116 and amber 0xFFFFB020 match the launcher icon.
 *
 * Dark (ink)                        Light (paper)
 *   background / surface  #0E1116     background / surface  #F7F3EC
 *   surfaceContainerLowest #0A0C10    surfaceContainerLowest #FFFFFF
 *   surfaceContainerLow   #151922     surfaceContainerLow   #F2EDE4
 *   surfaceContainer      #1B202A     surfaceContainer      #ECE6DB
 *   surfaceContainerHigh  #222833     surfaceContainerHigh  #E6DFD2
 *   surfaceContainerHighest #2A313D   surfaceContainerHighest #DFD7C8
 *   onSurface             #E6E9EF     onSurface             #1E1A14
 *   onSurfaceVariant      #9AA3B2     onSurfaceVariant      #5E5648
 *   outline               #3A4250     outline               #B9AF9E
 *   primary (amber)       #FFB020     primary (amber, deep) #8A5A00
 */

// Ink (dark ground)
val Ink = Color(0xFF0E1116)
val InkLowest = Color(0xFF0A0C10)
val InkLow = Color(0xFF151922)
val InkContainer = Color(0xFF1B202A)
val InkHigh = Color(0xFF222833)
val InkHighest = Color(0xFF2A313D)
val InkOnSurface = Color(0xFFE6E9EF)
val InkOnSurfaceVariant = Color(0xFF9AA3B2)
val InkOutline = Color(0xFF3A4250)
val InkOutlineVariant = Color(0xFF2A313D)

// Paper (light ground)
val Paper = Color(0xFFF7F3EC)
val PaperLowest = Color(0xFFFFFFFF)
val PaperLow = Color(0xFFF2EDE4)
val PaperContainer = Color(0xFFECE6DB)
val PaperHigh = Color(0xFFE6DFD2)
val PaperHighest = Color(0xFFDFD7C8)
val PaperOnSurface = Color(0xFF1E1A14)
val PaperOnSurfaceVariant = Color(0xFF5E5648)
val PaperOutline = Color(0xFFB9AF9E)
val PaperOutlineVariant = Color(0xFFDFD7C8)

// Amber accent
val Amber = Color(0xFFFFB020)
val AmberOn = Color(0xFF2A1A00)
val AmberContainerDark = Color(0xFF5C3D00)
val AmberOnContainerDark = Color(0xFFFFDDA6)
val AmberDeep = Color(0xFF8A5A00)
val AmberContainerLight = Color(0xFFFFDDA6)
val AmberOnContainerLight = Color(0xFF2A1A00)

internal val DoormanDarkColors: ColorScheme = darkColorScheme(
    primary = Amber,
    onPrimary = AmberOn,
    primaryContainer = AmberContainerDark,
    onPrimaryContainer = AmberOnContainerDark,
    secondary = InkOnSurfaceVariant,
    onSecondary = Ink,
    secondaryContainer = InkHighest,
    onSecondaryContainer = InkOnSurface,
    tertiary = Color(0xFF7FD3E6),
    onTertiary = Color(0xFF00313B),
    background = Ink,
    onBackground = InkOnSurface,
    surface = Ink,
    onSurface = InkOnSurface,
    surfaceVariant = InkHigh,
    onSurfaceVariant = InkOnSurfaceVariant,
    surfaceDim = Ink,
    surfaceBright = InkHighest,
    surfaceContainerLowest = InkLowest,
    surfaceContainerLow = InkLow,
    surfaceContainer = InkContainer,
    surfaceContainerHigh = InkHigh,
    surfaceContainerHighest = InkHighest,
    outline = InkOutline,
    outlineVariant = InkOutlineVariant,
    inverseSurface = InkOnSurface,
    inverseOnSurface = Ink,
    inversePrimary = AmberDeep,
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3B0907),
    scrim = Color.Black,
)

internal val DoormanLightColors: ColorScheme = lightColorScheme(
    primary = AmberDeep,
    onPrimary = Color.White,
    primaryContainer = AmberContainerLight,
    onPrimaryContainer = AmberOnContainerLight,
    secondary = PaperOnSurfaceVariant,
    onSecondary = Color.White,
    secondaryContainer = PaperHighest,
    onSecondaryContainer = PaperOnSurface,
    tertiary = Color(0xFF1F6676),
    onTertiary = Color.White,
    background = Paper,
    onBackground = PaperOnSurface,
    surface = Paper,
    onSurface = PaperOnSurface,
    surfaceVariant = PaperHigh,
    onSurfaceVariant = PaperOnSurfaceVariant,
    surfaceDim = PaperHighest,
    surfaceBright = PaperLowest,
    surfaceContainerLowest = PaperLowest,
    surfaceContainerLow = PaperLow,
    surfaceContainer = PaperContainer,
    surfaceContainerHigh = PaperHigh,
    surfaceContainerHighest = PaperHighest,
    outline = PaperOutline,
    outlineVariant = PaperOutlineVariant,
    inverseSurface = PaperOnSurface,
    inverseOnSurface = Paper,
    inversePrimary = Amber,
    error = Color(0xFFB3261E),
    onError = Color.White,
    scrim = Color.Black,
)
