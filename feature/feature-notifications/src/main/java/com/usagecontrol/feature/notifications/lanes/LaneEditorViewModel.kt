package com.usagecontrol.feature.notifications.lanes

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagecontrol.core.database.LaneRepository
import com.usagecontrol.core.model.Lane
import com.usagecontrol.core.ui.theme.DarkLaneColors
import androidx.compose.ui.graphics.toArgb
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LaneEditorUiState(
    val loading: Boolean = true,
    val draft: LaneDraft = LaneDraft(colorArgb = LaneEditorViewModel.palette.first()),
    val breakthroughsText: String = "",
    val error: String? = null,
    val saving: Boolean = false,
    val notFound: Boolean = false,
) {
    val isNew: Boolean get() = draft.id == 0L
}

/** Creates or edits one lane (R014). Route `lanes/{laneId}`; laneId 0 creates a new lane. */
@HiltViewModel
class LaneEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val laneRepository: LaneRepository,
) : ViewModel() {

    private val laneId: Long = savedStateHandle.get<Long>("laneId") ?: 0L

    private val _uiState = MutableStateFlow(LaneEditorUiState())
    val uiState: StateFlow<LaneEditorUiState> = _uiState.asStateFlow()

    /** Emits once after a successful save or delete so the screen can close. */
    private val _done = Channel<String>(Channel.BUFFERED)
    val done: Flow<String> = _done.receiveAsFlow()

    private var lanes: List<Lane> = emptyList()

    init {
        viewModelScope.launch {
            try {
                lanes = laneRepository.getLanes()
                val existing = lanes.firstOrNull { it.id == laneId }
                _uiState.value = when {
                    existing != null -> {
                        val draft = LaneEditorLogic.fromLane(existing)
                        LaneEditorUiState(loading = false, draft = draft, breakthroughsText = draft.breakthroughs.joinToString(", "))
                    }
                    laneId != 0L -> LaneEditorUiState(loading = false, notFound = true)
                    else -> LaneEditorUiState(
                        loading = false,
                        draft = LaneDraft(colorArgb = palette.first(), sortOrder = LaneEditorLogic.nextSortOrder(lanes)),
                    )
                }
                Log.d(TAG, "editor opened lane=$laneId found=${existing != null}")
            } catch (e: Exception) {
                Log.e(TAG, "editor load failed lane=$laneId", e)
                _uiState.value = LaneEditorUiState(loading = false, error = "Couldn't load the lane")
            }
        }
    }

    fun update(transform: (LaneDraft) -> LaneDraft) {
        _uiState.update { it.copy(draft = transform(it.draft), error = null) }
    }

    fun setBreakthroughsText(text: String) {
        _uiState.update {
            it.copy(breakthroughsText = text, draft = it.draft.copy(breakthroughs = LaneEditorLogic.parseBreakthroughs(text)))
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.saving || state.loading) return
        val error = LaneEditorLogic.validate(state.draft, lanes)
        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val id = laneRepository.saveLane(LaneEditorLogic.toLane(state.draft))
                Log.i(TAG, "lane saved id=$id name=${state.draft.name.trim()} policy=${state.draft.policy} decay=${state.draft.decayMinutes}")
                _done.send("Lane saved")
            } catch (e: Exception) {
                Log.e(TAG, "lane save failed id=${state.draft.id}", e)
                _uiState.update { it.copy(saving = false, error = "Couldn't save the lane") }
            }
        }
    }

    /** Deletes a custom lane; its routes are removed so senders fall back to their defaults. */
    fun delete() {
        val draft = _uiState.value.draft
        if (draft.isDefault || draft.id == 0L) return
        viewModelScope.launch {
            try {
                val removed = laneRepository.deleteLane(draft.id)
                Log.i(TAG, "lane delete id=${draft.id} removed=$removed")
                if (removed) _done.send("Lane deleted") else _uiState.update { it.copy(error = "Default lanes can't be deleted") }
            } catch (e: Exception) {
                Log.e(TAG, "lane delete failed id=${draft.id}", e)
                _uiState.update { it.copy(error = "Couldn't delete the lane") }
            }
        }
    }

    companion object {
        private const val TAG = "Lanes"

        /** Lane colour choices: the LaneColors palette (dark values, matching DefaultLanes). */
        val palette: List<Int> = DarkLaneColors.all.map { it.toArgb() }
    }
}
