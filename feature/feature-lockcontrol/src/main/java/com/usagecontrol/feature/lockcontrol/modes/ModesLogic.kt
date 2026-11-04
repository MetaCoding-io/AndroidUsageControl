package com.usagecontrol.feature.lockcontrol.modes

import com.usagecontrol.core.common.SoundProfileApplier.ApplyResult
import com.usagecontrol.core.database.dao.DailyUsageTotalRow
import com.usagecontrol.core.model.PresetTrigger
import com.usagecontrol.core.model.RingerMode
import com.usagecontrol.core.model.SoundPreset

/** Screen-time numbers shown on the Modes tab. */
data class ScreenTimeSummary(
    val todayMs: Long = 0,
    val avgDailyMs: Long = 0,
    val days: Int = 0,
)

/** What a toggle should do: apply [preset] (null = nothing to apply) and then mark [activeId]. */
data class ToggleAction(val preset: SoundPreset?, val activeId: Long?)

/** Pure Modes-tab rules, kept out of the ViewModel so they are JVM-testable. */
object ModesLogic {

    const val NORMAL_NAME = "Normal"

    /** MANUAL presets seeded when the sound_presets table is empty. */
    fun defaultPresets(): List<SoundPreset> = listOf(
        SoundPreset(
            name = "Focus",
            ringerMode = RingerMode.VIBRATE,
            notificationVolume = 0,
            triggerType = PresetTrigger.MANUAL,
        ),
        SoundPreset(
            name = "Sleep",
            ringerMode = RingerMode.SILENT,
            dndEnabled = true,
            triggerType = PresetTrigger.MANUAL,
        ),
        SoundPreset(
            name = NORMAL_NAME,
            ringerMode = RingerMode.NORMAL,
            ringVolume = 70,
            notificationVolume = 70,
            dndEnabled = false,
            triggerType = PresetTrigger.MANUAL,
        ),
    )

    /**
     * Turning a mode on applies it. Turning the active mode off reverts to the Normal preset
     * when one exists (and is not the mode being turned off); otherwise it only clears the
     * active mark and leaves the device as it is.
     */
    fun resolveToggle(
        presets: List<SoundPreset>,
        presetId: Long,
        enable: Boolean,
    ): ToggleAction? {
        val target = presets.firstOrNull { it.id == presetId } ?: return null
        if (enable) return ToggleAction(preset = target, activeId = target.id)
        val normal = presets.firstOrNull { it.name.equals(NORMAL_NAME, ignoreCase = true) }
        return if (normal != null && normal.id != target.id) {
            ToggleAction(preset = normal, activeId = null)
        } else {
            ToggleAction(preset = null, activeId = null)
        }
    }

    /** Snackbar text for a failed apply; null when nothing needs surfacing. */
    fun messageFor(presetName: String, result: ApplyResult): String? = when (result) {
        ApplyResult.Applied -> null
        ApplyResult.PermissionDenied ->
            "$presetName needs Do Not Disturb access. Grant it in Settings."
        is ApplyResult.Error -> "Couldn't apply $presetName: ${result.message}"
    }

    /** Today's total plus the average over the days that have usage rows. */
    fun screenTimeSummary(rows: List<DailyUsageTotalRow>, today: String): ScreenTimeSummary {
        if (rows.isEmpty()) return ScreenTimeSummary()
        return ScreenTimeSummary(
            todayMs = rows.firstOrNull { it.date == today }?.totalMs ?: 0,
            avgDailyMs = rows.sumOf { it.totalMs } / rows.size,
            days = rows.size,
        )
    }
}
