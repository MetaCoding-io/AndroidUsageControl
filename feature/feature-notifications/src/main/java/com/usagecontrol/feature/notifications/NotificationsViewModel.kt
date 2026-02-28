package com.usagecontrol.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.database.dao.ActionBreakdownRow
import com.usagecontrol.core.database.dao.DailyNotifStatsRow
import com.usagecontrol.core.database.dao.NotificationDao
import com.usagecontrol.core.database.dao.RuleDao
import com.usagecontrol.core.database.toEntity
import com.usagecontrol.core.database.toModel
import com.usagecontrol.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationDao: NotificationDao,
    private val ruleDao: RuleDao,
) : ViewModel() {

    private val _selectedApp = MutableStateFlow<String?>(null)
    val selectedApp: StateFlow<String?> = _selectedApp

    val apps = notificationDao.getDistinctAppsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications = _selectedApp.flatMapLatest { pkg ->
        if (pkg == null) {
            notificationDao.getAllFlow()
        } else {
            notificationDao.getByPackageFlow(pkg)
        }
    }.map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rules = ruleDao.getByPillarFlow(RulePillar.NOTIFICATION.name)
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard data
    private val _dailyStats = MutableStateFlow<List<DailyNotifStatsRow>>(emptyList())
    val dailyStats: StateFlow<List<DailyNotifStatsRow>> = _dailyStats

    private val _actionBreakdown = MutableStateFlow<ActionBreakdownRow?>(null)
    val actionBreakdown: StateFlow<ActionBreakdownRow?> = _actionBreakdown

    private val _dashboardDays = MutableStateFlow(7)
    val dashboardDays: StateFlow<Int> = _dashboardDays

    init {
        loadDashboard()
    }

    fun setDashboardDays(days: Int) {
        _dashboardDays.value = days
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            val since = TimeUtils.daysAgo(_dashboardDays.value)
            _dailyStats.value = notificationDao.getDailyStats(since)
            _actionBreakdown.value = notificationDao.getActionBreakdown(since)
        }
    }

    fun selectApp(packageName: String?) {
        _selectedApp.value = packageName
    }

    fun createDismissRule(appName: String, packageName: String) {
        viewModelScope.launch {
            val rule = Rule(
                name = "Auto-dismiss $appName",
                pillar = RulePillar.NOTIFICATION,
                conditions = listOf(
                    RuleCondition(
                        type = ConditionType.APP_PACKAGE,
                        params = mapOf("packageName" to packageName),
                    )
                ),
                actions = listOf(
                    RuleAction(type = ActionType.DISMISS_NOTIFICATION)
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
