package com.usagecontrol.feature.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.database.dao.NotificationDao
import com.usagecontrol.core.database.dao.RuleDao
import com.usagecontrol.core.database.dao.SuggestionDao
import com.usagecontrol.core.database.toEntity
import com.usagecontrol.core.database.toModel
import com.usagecontrol.core.model.AppDismissStats
import com.usagecontrol.core.model.RuleCreator
import com.usagecontrol.core.model.Suggestion
import com.usagecontrol.core.model.SuggestionStatus
import com.usagecontrol.core.rules.SuggestionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LearningViewModel @Inject constructor(
    private val notificationDao: NotificationDao,
    private val ruleDao: RuleDao,
    private val suggestionDao: SuggestionDao,
    private val suggestionEngine: SuggestionEngine,
) : ViewModel() {

    private val _stats = MutableStateFlow<List<AppDismissStats>>(emptyList())
    val stats: StateFlow<List<AppDismissStats>> = _stats

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _dayRange = MutableStateFlow(7)
    val dayRange: StateFlow<Int> = _dayRange

    val suggestions: StateFlow<List<Suggestion>> = suggestionDao.getPendingFlow()
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing

    init {
        loadStats()
    }

    fun setDayRange(days: Int) {
        _dayRange.value = days
        loadStats()
    }

    fun runAnalysis() {
        viewModelScope.launch {
            _isAnalyzing.value = true
            suggestionEngine.analyze()
            _isAnalyzing.value = false
        }
    }

    fun acceptSuggestion(suggestion: Suggestion) {
        viewModelScope.launch {
            val rule = suggestion.ruleTemplate.copy(
                createdBy = RuleCreator.LEARNED,
            )
            val ruleId = ruleDao.insert(rule.toEntity())
            suggestionDao.markAccepted(suggestion.id, ruleId)
        }
    }

    fun dismissSuggestion(suggestion: Suggestion) {
        viewModelScope.launch {
            suggestionDao.updateStatus(suggestion.id, SuggestionStatus.DISMISSED.name)
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            _isLoading.value = true
            val sinceTimestamp = TimeUtils.daysAgo(_dayRange.value)
            _stats.value = notificationDao.getDismissStats(sinceTimestamp)
                .map { it.toModel() }
                .sortedByDescending { it.dismissRate }
            _isLoading.value = false
        }
    }
}
