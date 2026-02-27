package com.usagecontrol.service.context

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import com.usagecontrol.core.model.RingerMode
import com.usagecontrol.core.model.SoundPreset
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies sound settings to the device.
 * Used both by SoundPresets (via auto-apply) and by rule-based actions.
 */
@Singleton
class SoundProfileExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val audioManager: AudioManager
        get() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Apply a full sound preset to the device.
     */
    fun applyPreset(preset: SoundPreset) {
        setRingerMode(preset.ringerMode)
        preset.alarmVolume?.let { setStreamVolume(AudioManager.STREAM_ALARM, it) }
        preset.ringVolume?.let { setStreamVolume(AudioManager.STREAM_RING, it) }
        preset.notificationVolume?.let { setStreamVolume(AudioManager.STREAM_NOTIFICATION, it) }
        preset.mediaVolume?.let { setStreamVolume(AudioManager.STREAM_MUSIC, it) }
        preset.dndEnabled?.let { setDnd(it) }
    }

    /**
     * Set the ringer mode.
     */
    fun setRingerMode(mode: RingerMode) {
        try {
            audioManager.ringerMode = when (mode) {
                RingerMode.SILENT -> AudioManager.RINGER_MODE_SILENT
                RingerMode.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
                RingerMode.NORMAL -> AudioManager.RINGER_MODE_NORMAL
            }
        } catch (_: SecurityException) {
            // May need DND access policy permission
        }
    }

    /**
     * Set ringer mode from string (used by rule actions).
     */
    fun setRingerModeFromString(mode: String) {
        val ringerMode = when (mode.lowercase()) {
            "silent" -> RingerMode.SILENT
            "vibrate" -> RingerMode.VIBRATE
            "normal" -> RingerMode.NORMAL
            else -> return
        }
        setRingerMode(ringerMode)
    }

    /**
     * Set volume for a specific audio stream.
     * @param stream AudioManager stream constant
     * @param percent Volume level 0-100
     */
    fun setStreamVolume(stream: Int, percent: Int) {
        try {
            val maxVol = audioManager.getStreamMaxVolume(stream)
            val volume = (percent * maxVol / 100).coerceIn(0, maxVol)
            audioManager.setStreamVolume(stream, volume, 0)
        } catch (_: SecurityException) {
            // Volume control restricted
        }
    }

    /**
     * Set volume by stream name string (used by rule actions).
     * @param streamName One of: "alarm", "ring", "notification", "media"
     * @param percent Volume level 0-100
     */
    fun setStreamVolumeByName(streamName: String, percent: Int) {
        val stream = when (streamName.lowercase()) {
            "alarm" -> AudioManager.STREAM_ALARM
            "ring" -> AudioManager.STREAM_RING
            "notification" -> AudioManager.STREAM_NOTIFICATION
            "media" -> AudioManager.STREAM_MUSIC
            else -> return
        }
        setStreamVolume(stream, percent)
    }

    /**
     * Enable or disable Do Not Disturb.
     */
    fun setDnd(enabled: Boolean) {
        try {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(
                    if (enabled) NotificationManager.INTERRUPTION_FILTER_NONE
                    else NotificationManager.INTERRUPTION_FILTER_ALL
                )
            }
        } catch (_: SecurityException) {
            // DND access not granted
        }
    }
}
