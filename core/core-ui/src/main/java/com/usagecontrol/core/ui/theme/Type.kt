package com.usagecontrol.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.usagecontrol.core.ui.R

// Fonts are OFL-1.1; licences in core/core-ui/licenses/.
// manrope.ttf is the variable (wght 200..800) font; DM Mono has no variable cut,
// so the static Regular and Medium files are bundled.
private fun manrope(weight: FontWeight) = Font(
    resId = R.font.manrope,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Manrope = FontFamily(
    manrope(FontWeight.Normal),
    manrope(FontWeight.Medium),
    manrope(FontWeight.SemiBold),
    manrope(FontWeight.Bold),
    manrope(FontWeight.ExtraBold),
)

val DmMono = FontFamily(
    Font(R.font.dm_mono, FontWeight.Normal),
    Font(R.font.dm_mono_medium, FontWeight.Medium),
)

private const val TabularNumbers = "tnum"

private val base = Typography()

private fun TextStyle.ui(weight: FontWeight) = copy(fontFamily = Manrope, fontWeight = weight)

val DoormanTypography = Typography(
    displayLarge = base.displayLarge.ui(FontWeight.ExtraBold),
    displayMedium = base.displayMedium.ui(FontWeight.ExtraBold),
    displaySmall = base.displaySmall.ui(FontWeight.Bold),
    headlineLarge = base.headlineLarge.ui(FontWeight.Bold),
    headlineMedium = base.headlineMedium.ui(FontWeight.Bold),
    headlineSmall = base.headlineSmall.ui(FontWeight.SemiBold),
    titleLarge = base.titleLarge.ui(FontWeight.SemiBold),
    titleMedium = base.titleMedium.ui(FontWeight.SemiBold),
    titleSmall = base.titleSmall.ui(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.ui(FontWeight.Normal),
    bodyMedium = base.bodyMedium.ui(FontWeight.Normal),
    bodySmall = base.bodySmall.ui(FontWeight.Normal),
    labelLarge = base.labelLarge.ui(FontWeight.SemiBold),
    labelMedium = base.labelMedium.ui(FontWeight.SemiBold),
    labelSmall = base.labelSmall.ui(FontWeight.Medium),
)

/** Monospace styles for counts, times and durations. All use tabular figures. */
object DoormanMonoType {
    val hero = TextStyle(
        fontFamily = DmMono,
        fontWeight = FontWeight.Medium,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1).sp,
        fontFeatureSettings = TabularNumbers,
    )
    val value = TextStyle(
        fontFamily = DmMono,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = TabularNumbers,
    )
    val meta = TextStyle(
        fontFamily = DmMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
        fontFeatureSettings = TabularNumbers,
    )
}
