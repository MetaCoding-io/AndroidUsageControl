package com.usagecontrol.service.context

import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.database.dao.SoundPresetDao
import com.usagecontrol.core.database.toModel
import com.usagecontrol.core.model.PresetTrigger
import com.usagecontrol.core.model.SoundPreset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evaluates sound presets against the current context and applies matching ones.
 * Called by ContextMonitorService on each poll cycle.
 */
@Singleton
class SoundPresetApplier @Inject constructor(
    private val soundPresetDao: SoundPresetDao,
    private val soundProfileExecutor: SoundProfileExecutor,
) {

    private var lastAppliedPresetId: Long? = null

    /**
     * Check enabled presets and apply the first matching one.
     * @param currentWifiSsid The current WiFi SSID (null if not connected)
     */
    suspend fun evaluateAndApply(currentWifiSsid: String?) {
        val enabledPresets = soundPresetDao.getEnabled().map { it.toModel() }

        val matchingPreset = enabledPresets.firstOrNull { preset ->
            matches(preset, currentWifiSsid)
        }

        if (matchingPreset != null && matchingPreset.id != lastAppliedPresetId) {
            soundProfileExecutor.applyPreset(matchingPreset)
            lastAppliedPresetId = matchingPreset.id
        } else if (matchingPreset == null) {
            lastAppliedPresetId = null
        }
    }

    private fun matches(preset: SoundPreset, currentWifiSsid: String?): Boolean {
        return when (preset.triggerType) {
            PresetTrigger.TIME_RANGE -> {
                val start = preset.triggerParams["startTime"] ?: return false
                val end = preset.triggerParams["endTime"] ?: return false
                val current = TimeUtils.currentTimeOfDay()
                TimeUtils.isTimeInRange(current, start, end)
            }
            PresetTrigger.WIFI_CONNECTED -> {
                val ssid = preset.triggerParams["ssid"] ?: return false
                currentWifiSsid == ssid
            }
            PresetTrigger.MANUAL -> false // Manual presets are never auto-applied
        }
    }
}
