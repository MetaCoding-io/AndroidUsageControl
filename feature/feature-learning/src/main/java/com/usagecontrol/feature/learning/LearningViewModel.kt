package com.usagecontrol.feature.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.database.dao.NotificationDao
import com.usagecontrol.core.database.toModel
import com.usagecontrol.core.model.AppDismissStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LearningViewModel @Inject constructor(
    private val notificationDao: NotificationDao,
) : ViewModel() {

    private val _stats = MutableStateFlow<List<AppDismissStats>>(emptyList())
    val stats: StateFlow<List<AppDismissStats>> = _stats

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _dayRange = MutableStateFlow(7)
    val dayRange: StateFlow<Int> = _dayRange

    init {
        loadStats()
    }

    fun setDayRange(days: Int) {
        _dayRange.value = days
        loadStats()
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
