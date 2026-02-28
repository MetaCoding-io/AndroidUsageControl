package com.usagecontrol.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagecontrol.core.database.dao.SoundPresetDao
import com.usagecontrol.core.database.toEntity
import com.usagecontrol.core.model.PresetTrigger
import com.usagecontrol.core.model.RingerMode
import com.usagecontrol.core.model.SoundPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuickSetupViewModel @Inject constructor(
    private val soundPresetDao: SoundPresetDao,
) : ViewModel() {

    fun createNightPreset(onDone: () -> Unit) {
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
            onDone()
        }
    }
}
