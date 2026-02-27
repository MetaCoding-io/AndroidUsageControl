package com.usagecontrol.feature.lockcontrol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagecontrol.core.database.dao.ContextSnapshotDao
import com.usagecontrol.core.database.dao.SoundPresetDao
import com.usagecontrol.core.database.toEntity
import com.usagecontrol.core.database.toModel
import com.usagecontrol.core.model.ContextSnapshot
import com.usagecontrol.core.model.PresetTrigger
import com.usagecontrol.core.model.RingerMode
import com.usagecontrol.core.model.SoundPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockControlViewModel @Inject constructor(
    private val soundPresetDao: SoundPresetDao,
    private val contextSnapshotDao: ContextSnapshotDao,
) : ViewModel() {

    val presets = soundPresetDao.getAllFlow()
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentContext = MutableStateFlow<ContextSnapshot?>(null)
    val currentContext: StateFlow<ContextSnapshot?> = _currentContext

    init {
        loadCurrentContext()
    }

    private fun loadCurrentContext() {
        viewModelScope.launch {
            val latest = contextSnapshotDao.getLatest()
            _currentContext.value = latest?.toModel()
        }
    }

    fun refreshContext() = loadCurrentContext()

    fun createNightPreset() {
        viewModelScope.launch {
            val preset = SoundPreset(
                name = "Night Mode",
                enabled = true,
                ringerMode = RingerMode.SILENT,
                alarmVolume = 100,
                ringVolume = 0,
                notificationVolume = 0,
                mediaVolume = 0,
                dndEnabled = true,
                triggerType = PresetTrigger.TIME_RANGE,
                triggerParams = mapOf("startTime" to "22:00", "endTime" to "07:00"),
            )
            soundPresetDao.insert(preset.toEntity())
        }
    }

    fun createMeetingPreset() {
        viewModelScope.launch {
            val preset = SoundPreset(
                name = "Meeting Mode",
                enabled = true,
                ringerMode = RingerMode.VIBRATE,
                alarmVolume = null,
                ringVolume = 0,
                notificationVolume = 0,
                mediaVolume = 0,
                dndEnabled = true,
                triggerType = PresetTrigger.MANUAL,
                triggerParams = emptyMap(),
            )
            soundPresetDao.insert(preset.toEntity())
        }
    }

    fun createHomePreset(wifiSsid: String) {
        viewModelScope.launch {
            val preset = SoundPreset(
                name = "Home (WiFi: $wifiSsid)",
                enabled = true,
                ringerMode = RingerMode.NORMAL,
                alarmVolume = null,
                ringVolume = null,
                notificationVolume = null,
                mediaVolume = null,
                dndEnabled = false,
                triggerType = PresetTrigger.WIFI_CONNECTED,
                triggerParams = mapOf("ssid" to wifiSsid),
            )
            soundPresetDao.insert(preset.toEntity())
        }
    }

    fun createCustomPreset(
        name: String,
        ringerMode: RingerMode,
        alarmVolume: Int?,
        ringVolume: Int?,
        notificationVolume: Int?,
        mediaVolume: Int?,
        dndEnabled: Boolean?,
        triggerType: PresetTrigger,
        triggerParams: Map<String, String>,
    ) {
        viewModelScope.launch {
            val preset = SoundPreset(
                name = name,
                enabled = true,
                ringerMode = ringerMode,
                alarmVolume = alarmVolume,
                ringVolume = ringVolume,
                notificationVolume = notificationVolume,
                mediaVolume = mediaVolume,
                dndEnabled = dndEnabled,
                triggerType = triggerType,
                triggerParams = triggerParams,
            )
            soundPresetDao.insert(preset.toEntity())
        }
    }

    fun togglePreset(preset: SoundPreset) {
        viewModelScope.launch {
            soundPresetDao.update(preset.copy(enabled = !preset.enabled).toEntity())
        }
    }

    fun deletePreset(preset: SoundPreset) {
        viewModelScope.launch {
            soundPresetDao.delete(preset.toEntity())
        }
    }
}
