package com.usagecontrol.feature.notifications.apps

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagecontrol.core.common.TimeUtils
import com.usagecontrol.core.database.dao.NotificationDao
import com.usagecontrol.core.database.dao.RuleDao
import com.usagecontrol.core.database.dao.SuggestionDao
import com.usagecontrol.core.database.dao.UsageDao
import com.usagecontrol.core.database.toEntity
import com.usagecontrol.core.database.toModel
import com.usagecontrol.core.model.Rule
import com.usagecontrol.core.model.RuleCreator
import com.usagecontrol.core.model.RulePillar
import com.usagecontrol.core.model.Suggestion
import com.usagecontrol.core.model.SuggestionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class AppProfileUiState(
    val loading: Boolean = true,
    val packageName: String = "",
    val label: String = "",
    val windowDays: Int = AppProfileViewModel.WINDOW_DAYS,
    val total: Int = 0,
    val dismissed: Int = 0,
    val medianTimeToActMs: Long? = null,
    val hourly: List<Int> = List(24) { 0 },
    val channels: List<Pair<String, Int>> = emptyList(),
    val todayScreenTimeMs: Long = 0,
    val limitMinutes: Int? = null,
    val suggestions: List<Suggestion> = emptyList(),
    val error: Boolean = false,
) {
    /** Everything the user did not throw away: opened, snoozed, or left for later. */
    val acted: Int get() = (total - dismissed).coerceAtLeast(0)
}

@HiltViewModel
class AppProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val notificationDao: NotificationDao,
    private val usageDao: UsageDao,
    private val ruleDao: RuleDao,
    private val suggestionDao: SuggestionDao,
) : ViewModel() {

    val packageName: String = savedStateHandle.get<String>("packageName").orEmpty()

    private val zone: ZoneId = ZoneId.systemDefault()

    // Fixed when the screen opens; the profile is short-lived, so no midnight roll is needed.
    private val since = LocalDate.now(zone).minusDays(WINDOW_DAYS - 1L)
        .atStartOfDay(zone).toInstant().toEpochMilli()

    /** Resolved once; the PackageManager lookup runs on the IO-bound upstream. */
    private var cachedLabel: String? = null

    private fun labelFor(storedName: String?): String =
        cachedLabel ?: resolveLabel(context, packageName, storedName).also { cachedLabel = it }

    private val counts = notificationDao.appAlertCountsFlow(since)
        .map { rows -> rows.firstOrNull { it.packageName == packageName } }

    private val events = notificationDao.appEventsFlow(packageName, since)
    private val channels = notificationDao.appChannelCountsFlow(packageName, since)

    private val limitRule: Flow<Rule?> = ruleDao.getByPillarFlow(RulePillar.SCREEN_TIME.name)
        .map { list -> AppsLogic.findLimitRule(list.map { it.toModel() }, packageName) }

    private val suggestions = suggestionDao.getPendingFlow()
        .map { list -> list.filter { it.appPackageName == packageName }.map { it.toModel() } }

    private val todayUsage = usageDao.getByDateFlow(TimeUtils.todayDate())
        .map { list -> list.filter { it.packageName == packageName }.sumOf { it.foregroundTimeMs } }

    private val stats = combine(counts, events, channels) { c, e, ch ->
        AppProfileUiState(
            loading = false,
            packageName = packageName,
            label = labelFor(c?.appName),
            total = c?.total ?: 0,
            dismissed = c?.dismissed ?: 0,
            medianTimeToActMs = AppStats.medianTimeToAct(e),
            hourly = AppStats.hourlyHistogram(e.map { it.timestamp }, zone),
            channels = AppStats.channelBreakdown(ch),
        )
    }

    val uiState: StateFlow<AppProfileUiState> =
        combine(stats, limitRule, suggestions, todayUsage) { s, rule, sug, usage ->
            s.copy(
                limitMinutes = rule?.let(AppsLogic::limitMinutes),
                suggestions = sug,
                todayScreenTimeMs = usage,
            )
        }.flowOn(Dispatchers.IO)
            .catch { e ->
                Log.e(TAG, "profile query failed pkg=$packageName", e)
                emit(AppProfileUiState(loading = false, packageName = packageName, label = packageName, error = true))
            }
            .onEach { s ->
                Log.d(TAG, "state pkg=$packageName total=${s.total} limit=${s.limitMinutes} sug=${s.suggestions.size}")
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppProfileUiState(packageName = packageName))

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    /** Creates or updates this app's daily screen-time limit rule. Non-positive minutes are rejected. */
    fun setLimit(minutes: Int) {
        if (minutes <= 0) {
            viewModelScope.launch { _messages.send("Limit must be at least 1 minute") }
            return
        }
        viewModelScope.launch {
            try {
                val appName = uiState.value.label.ifBlank { packageName }
                val existing = AppsLogic.findLimitRule(
                    ruleDao.getByPillarFlow(RulePillar.SCREEN_TIME.name).first().map { it.toModel() },
                    packageName,
                )
                if (existing != null) {
                    ruleDao.update(AppsLogic.withLimit(existing, appName, minutes, System.currentTimeMillis()).toEntity())
                    Log.i(TAG, "limit pkg=$packageName rule=${existing.id}: updated to ${minutes}min")
                } else {
                    val id = ruleDao.insert(AppsLogic.newLimitRule(appName, packageName, minutes).toEntity())
                    Log.i(TAG, "limit pkg=$packageName rule=$id: created ${minutes}min")
                }
                _messages.send("Daily limit set to ${minutes} min")
            } catch (e: Exception) {
                Log.e(TAG, "limit pkg=$packageName: save failed", e)
                _messages.send("Couldn't save the limit")
            }
        }
    }

    /** Deletes this app's limit rule, if any. */
    fun clearLimit() {
        viewModelScope.launch {
            try {
                val existing = AppsLogic.findLimitRule(
                    ruleDao.getByPillarFlow(RulePillar.SCREEN_TIME.name).first().map { it.toModel() },
                    packageName,
                ) ?: return@launch
                ruleDao.delete(existing.toEntity())
                Log.i(TAG, "limit pkg=$packageName rule=${existing.id}: removed")
                _messages.send("Daily limit removed")
            } catch (e: Exception) {
                Log.e(TAG, "limit pkg=$packageName: remove failed", e)
                _messages.send("Couldn't remove the limit")
            }
        }
    }

    /** Same as LearningViewModel.acceptSuggestion: insert the template as a LEARNED rule. */
    fun acceptSuggestion(suggestion: Suggestion) {
        viewModelScope.launch {
            try {
                val ruleId = ruleDao.insert(suggestion.ruleTemplate.copy(createdBy = RuleCreator.LEARNED).toEntity())
                suggestionDao.markAccepted(suggestion.id, ruleId)
                Log.i(TAG, "suggestion ${suggestion.id} accepted as rule $ruleId")
                _messages.send("Rule added")
            } catch (e: Exception) {
                Log.e(TAG, "suggestion ${suggestion.id}: accept failed", e)
                _messages.send("Couldn't add the rule")
            }
        }
    }

    fun dismissSuggestion(suggestion: Suggestion) {
        viewModelScope.launch {
            suggestionDao.updateStatus(suggestion.id, SuggestionStatus.DISMISSED.name)
            Log.i(TAG, "suggestion ${suggestion.id} dismissed")
        }
    }

    companion object {
        const val WINDOW_DAYS = 30
        private const val TAG = "AppProfileViewModel"
    }
}
