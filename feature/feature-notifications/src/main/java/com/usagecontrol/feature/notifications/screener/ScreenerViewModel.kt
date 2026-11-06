package com.usagecontrol.feature.notifications.screener

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagecontrol.core.database.LaneRepository
import com.usagecontrol.core.database.dao.NotificationDao
import com.usagecontrol.core.model.Lane
import com.usagecontrol.core.model.RouteKind
import com.usagecontrol.core.rules.ReScreenPrompt
import com.usagecontrol.core.rules.ReScreenPromptEngine
import com.usagecontrol.feature.notifications.lanes.LaneLogic
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScreenerUiState(
    val loading: Boolean = true,
    val lanes: List<Lane> = emptyList(),
    val senders: List<UnscreenedSender> = emptyList(),
    val prompts: List<ReScreenPrompt> = emptyList(),
    /** Sender ids with a route write in flight; their chips are disabled. */
    val busy: Set<String> = emptySet(),
    val error: Boolean = false,
)

/** Backs the Screener sheet (R014): unscreened senders plus habit re-screening prompts (R004). */
@HiltViewModel
class ScreenerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    notificationDao: NotificationDao,
    private val laneRepository: LaneRepository,
) : ViewModel() {

    private val since = System.currentTimeMillis() - LaneLogic.WINDOW_DAYS * DAY_MS

    private val focusPackage = MutableStateFlow<String?>(null)
    private val dismissedPrompts = MutableStateFlow<Set<String>>(emptySet())
    private val busy = MutableStateFlow<Set<String>>(emptySet())

    private val promptInputs = combine(
        notificationDao.appQuickDismissStatsFlow(since, ReScreenPromptEngine.QUICK_DISMISS_MS),
        notificationDao.appLaneCountsFlow(since),
        dismissedPrompts,
        ::Triple,
    )

    val uiState: StateFlow<ScreenerUiState> = combine(
        laneRepository.lanes,
        laneRepository.routes,
        combine(notificationDao.unroutedFlow(since, context.packageName), focusPackage, ::Pair),
        promptInputs,
        busy,
    ) { lanes, routes, (rows, focus), (quick, laneCounts, dismissed), inFlight ->
        ScreenerUiState(
            loading = false,
            lanes = lanes,
            senders = ScreenerLogic.unscreenedSenders(rows, focus),
            prompts = LaneLogic.prompts(quick, routes, laneCounts, lanes, dismissed),
            busy = inFlight,
        )
    }.flowOn(Dispatchers.IO)
        .catch { e ->
            Log.e(TAG, "screener query failed", e)
            emit(ScreenerUiState(loading = false, error = true))
        }
        .onEach { s -> Log.d(TAG, "screener senders=${s.senders.size} prompts=${s.prompts.size} lanes=${s.lanes.size}") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScreenerUiState())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    /** Moves the app whose Screener notification was tapped to the top of the list. */
    fun focus(packageName: String?) {
        focusPackage.value = packageName
    }

    /** Creates the sender's route and re-files its history into [lane]. */
    fun route(sender: UnscreenedSender, lane: Lane) {
        routeInternal(sender.id, sender.kind, sender.packageName, sender.key, lane, sender.displayName)
    }

    /** Accepts a re-screening prompt: route the whole app to the suggested lane. */
    fun acceptPrompt(prompt: ReScreenPrompt) {
        val lane = uiState.value.lanes.firstOrNull { it.id == prompt.targetLaneId }
        if (lane == null) {
            Log.w(TAG, "prompt ${prompt.packageName}: target lane ${prompt.targetLaneId} missing")
            viewModelScope.launch { _messages.send("That lane no longer exists") }
            return
        }
        routeInternal("APP|${prompt.packageName}|", RouteKind.APP, prompt.packageName, "", lane, prompt.appName)
    }

    /** Hides a prompt for the lifetime of this sheet. */
    fun dismissPrompt(prompt: ReScreenPrompt) {
        dismissedPrompts.update { it + prompt.packageName }
        Log.i(TAG, "prompt ${prompt.packageName} dismissed")
    }

    private fun routeInternal(id: String, kind: RouteKind, packageName: String, key: String, lane: Lane, label: String) {
        if (id in busy.value) return
        busy.update { it + id }
        viewModelScope.launch {
            try {
                val reFiled = laneRepository.setRoute(kind, packageName, key, lane.id)
                Log.i(TAG, "screener sheet routed kind=$kind pkg=$packageName key=$key lane=${lane.id} reFiled=$reFiled")
                _messages.send("$label → ${lane.name} · re-filed $reFiled")
            } catch (e: Exception) {
                Log.e(TAG, "screener sheet route failed kind=$kind pkg=$packageName lane=${lane.id}", e)
                _messages.send("Couldn't route $label")
            } finally {
                busy.update { it - id }
            }
        }
    }

    companion object {
        private const val TAG = "Lanes"
        private const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
