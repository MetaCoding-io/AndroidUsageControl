package com.usagecontrol.feature.lockcontrol.modes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagecontrol.core.common.SoundProfileApplier
import com.usagecontrol.core.common.SoundProfileApplier.ApplyResult
import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.database.dao.SoundPresetDao
import com.usagecontrol.core.database.dao.UsageDao
import com.usagecontrol.core.database.toEntity
import com.usagecontrol.core.database.toModel
import com.usagecontrol.core.model.SoundPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ModesUiState(
    val loading: Boolean = true,
    val presets: List<SoundPreset> = emptyList(),
    val activeId: Long? = null,
    val applyingId: Long? = null,
    val screenTime: ScreenTimeSummary = ScreenTimeSummary(),
    val error: Boolean = false,
) {
    val activePreset: SoundPreset? get() = presets.firstOrNull { it.id == activeId }
}

/**
 * Modes tab: sound presets as toggleable cards.
 *
 * The active mode is tracked here, not persisted. Known v1 limitation: auto-applied
 * (non-MANUAL: TIME_RANGE / WIFI_CONNECTED) presets are applied by SoundPresetApplier in
 * ContextMonitorService on its own poll cycle and may override a manual pick made here; this
 * screen will still show the manual pick as active.
 */
@HiltViewModel
class ModesViewModel @Inject constructor(
    private val soundPresetDao: SoundPresetDao,
    private val usageDao: UsageDao,
    private val applier: SoundProfileApplier,
) : ViewModel() {

    private val activeId = MutableStateFlow<Long?>(null)
    private val applyingId = MutableStateFlow<Long?>(null)
    private val screenTime = MutableStateFlow(ScreenTimeSummary())
    private var seeded = false

    private val messages = Channel<String>(Channel.BUFFERED)

    /** One-shot snackbar messages (permission denied, apply errors). */
    val snackbar: Flow<String> = messages.receiveAsFlow()

    private val presets: Flow<List<SoundPreset>> = soundPresetDao.getAllFlow()
        .onEach { rows -> if (rows.isEmpty()) seedDefaults() }
        .map { rows -> rows.map { it.toModel() } }

    val uiState: StateFlow<ModesUiState> = combine(
        presets, activeId, applyingId, screenTime,
    ) { list, active, applying, usage ->
        ModesUiState(
            loading = false,
            presets = list,
            activeId = active,
            applyingId = applying,
            screenTime = usage,
        )
    }.flowOn(Dispatchers.IO)
        .catch { e ->
            Log.e(TAG, "presets query failed", e)
            emit(ModesUiState(loading = false, error = true))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ModesUiState())

    init {
        loadScreenTime()
    }

    fun onToggle(presetId: Long, enable: Boolean) {
        if (applyingId.value != null) return
        val action = ModesLogic.resolveToggle(uiState.value.presets, presetId, enable) ?: return
        val preset = action.preset
        if (preset == null) {
            activeId.value = action.activeId
            return
        }
        applyingId.value = presetId
        viewModelScope.launch {
            val result = applier.apply(preset)
            Log.d(TAG, "toggle preset=$presetId enable=$enable applied=${preset.id} result=$result")
            if (result == ApplyResult.Applied) activeId.value = action.activeId
            ModesLogic.messageFor(preset.name, result)?.let { messages.send(it) }
            applyingId.value = null
        }
    }

    private suspend fun seedDefaults() {
        if (seeded) return
        seeded = true
        Log.i(TAG, "sound_presets empty; seeding default MANUAL presets")
        ModesLogic.defaultPresets().forEach { soundPresetDao.insert(it.toEntity()) }
    }

    private fun loadScreenTime() {
        viewModelScope.launch {
            screenTime.value = try {
                withContext(Dispatchers.IO) {
                    val today = TimeUtils.todayDate()
                    val rows = usageDao.getDailyTotals(TimeUtils.dateOffset(-(SUMMARY_DAYS - 1)), today)
                    ModesLogic.screenTimeSummary(rows, today)
                }
            } catch (e: Exception) {
                Log.e(TAG, "screen time query failed", e)
                ScreenTimeSummary()
            }
        }
    }

    companion object {
        const val SUMMARY_DAYS = 7
        private const val TAG = "ModesViewModel"
    }
}
