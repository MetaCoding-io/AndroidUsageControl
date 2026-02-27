package com.usagecontrol.feature.screentime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.database.dao.RuleDao
import com.usagecontrol.core.database.dao.UsageDao
import com.usagecontrol.core.database.toEntity
import com.usagecontrol.core.database.toModel
import com.usagecontrol.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScreenTimeViewModel @Inject constructor(
    private val usageDao: UsageDao,
    private val ruleDao: RuleDao,
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(TimeUtils.todayDate())
    val selectedDate: StateFlow<String> = _selectedDate

    val usageRecords = _selectedDate.flatMapLatest { date ->
        usageDao.getByDateFlow(date)
    }.map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rules = ruleDao.getByPillarFlow(RulePillar.SCREEN_TIME.name)
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalScreenTime = usageRecords.map { records ->
        records.sumOf { it.foregroundTimeMs }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun selectDate(date: String) {
        _selectedDate.value = date
    }

    fun createTimeLimitRule(appName: String, packageName: String, dailyLimitMinutes: Int) {
        viewModelScope.launch {
            val rule = Rule(
                name = "$appName: ${dailyLimitMinutes}min/day",
                pillar = RulePillar.SCREEN_TIME,
                conditions = listOf(
                    RuleCondition(
                        type = ConditionType.APP_PACKAGE,
                        params = mapOf("packageName" to packageName),
                    )
                ),
                actions = listOf(
                    RuleAction(
                        type = ActionType.ADVISE,
                        params = mapOf("dailyLimitMinutes" to dailyLimitMinutes.toString()),
                    )
                ),
            )
            ruleDao.insert(rule.toEntity())
        }
    }

    fun toggleRule(ruleId: Long, enabled: Boolean) {
        viewModelScope.launch {
            ruleDao.setEnabled(ruleId, enabled)
        }
    }

    fun deleteRule(rule: Rule) {
        viewModelScope.launch {
            ruleDao.delete(rule.toEntity())
        }
    }
}
