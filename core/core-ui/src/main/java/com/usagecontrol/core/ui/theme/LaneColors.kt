package com.usagecontrol.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The six notification lanes. Order is the canonical display order. */
enum class Lane { People, Money, Deliveries, Work, Live, Noise }

/**
 * Semantic lane colours. Dark values are saturated so they read as LEDs on ink;
 * light values are de-saturated/deepened so they hold contrast on paper.
 */
@Immutable
data class LaneColors(
    val people: Color,
    val money: Color,
    val deliveries: Color,
    val work: Color,
    val live: Color,
    val noise: Color,
) {
    operator fun get(lane: Lane): Color = when (lane) {
        Lane.People -> people
        Lane.Money -> money
        Lane.Deliveries -> deliveries
        Lane.Work -> work
        Lane.Live -> live
        Lane.Noise -> noise
    }

    val all: List<Color> get() = Lane.entries.map(::get)
}

val DarkLaneColors = LaneColors(
    people = Color(0xFFFF6B5A),     // coral
    money = Color(0xFFFFB020),      // amber
    deliveries = Color(0xFF2ED3E6), // cyan
    work = Color(0xFF9B7BFF),       // violet
    live = Color(0xFFB4F03C),       // lime
    noise = Color(0xFF8A94A6),      // slate
)

val LightLaneColors = LaneColors(
    people = Color(0xFFB8544A),
    money = Color(0xFF9A6A1C),
    deliveries = Color(0xFF2E7F8A),
    work = Color(0xFF6A5AA8),
    live = Color(0xFF5E7F2A),
    noise = Color(0xFF6B7280),
)

fun laneColors(darkTheme: Boolean): LaneColors =
    if (darkTheme) DarkLaneColors else LightLaneColors

val LocalLaneColors = staticCompositionLocalOf { DarkLaneColors }
