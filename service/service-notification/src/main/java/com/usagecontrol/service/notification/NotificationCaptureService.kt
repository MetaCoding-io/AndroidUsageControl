package com.usagecontrol.service.notification

import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.usagecontrol.core.common.AppNameResolver
import com.usagecontrol.core.database.LaneRepository
import com.usagecontrol.core.database.dao.LiveNotificationDao
import com.usagecontrol.core.database.dao.NotificationDao
import com.usagecontrol.core.database.entity.LiveNotificationEntity
import com.usagecontrol.core.database.entity.NotificationEntity
import com.usagecontrol.core.model.ActionType
import com.usagecontrol.core.model.NotificationAction
import com.usagecontrol.core.model.Route
import com.usagecontrol.core.model.RulePillar
import com.usagecontrol.core.rules.EvaluationContext
import com.usagecontrol.core.rules.LaneInput
import com.usagecontrol.core.rules.LaneResolver
import com.usagecontrol.core.rules.Resolution
import com.usagecontrol.core.rules.ProvisionalLaneClassifier
import com.usagecontrol.core.rules.RuleEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * Captures notifications into history (keyed by StatusBarNotification key, one row per
 * key with an update count and removal reason) and keeps ongoing / FGS / media
 * notifications in the live table instead.
 *
 * All callbacks are applied on a single-threaded dispatcher so a post, its updates and
 * its removal always hit the database in arrival order.
 */
@AndroidEntryPoint
class NotificationCaptureService : NotificationListenerService() {

    @Inject lateinit var notificationDao: NotificationDao
    @Inject lateinit var liveNotificationDao: LiveNotificationDao
    @Inject lateinit var ruleEngine: RuleEngine
    @Inject lateinit var iconStore: IconStore
    @Inject lateinit var pendingIntentCache: PendingIntentCache
    @Inject lateinit var laneRepository: LaneRepository
    @Inject lateinit var screenerNotifier: ScreenerNotifier

    private val errorHandler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "capture failed", e)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1) + errorHandler)

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap?) {
        scope.launch { handlePosted(sbn, rankingMap, applyRules = true) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap?, reason: Int) {
        scope.launch { handleRemoved(sbn, reason) }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        scope.launch { reconcile() }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance === this) instance = null
        // PendingIntents from a previous binding may be stale; reconcile re-caches on reconnect.
        scope.launch {
            val cleared = pendingIntentCache.size
            pendingIntentCache.clear()
            Log.i(TAG, "disconnected: cleared $cleared cached intents")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
        scope.cancel()
    }

    // -- handlers (run on the serial dispatcher) --

    private suspend fun handlePosted(sbn: StatusBarNotification, rankingMap: RankingMap?, applyRules: Boolean) {
        val notification = sbn.notification ?: return
        val key = sbn.key
        if (ScreenerMapping.isOwnScreenerPost(sbn.packageName, notification.channelId, packageName)) return

        cacheIntents(key, notification)

        if (isLive(notification)) {
            liveNotificationDao.upsert(buildLive(sbn, notification))
            Log.d(TAG, "live upsert key=$key pkg=${sbn.packageName}")
            return
        }
        // A notification that stopped being ongoing leaves the live table.
        liveNotificationDao.deleteByKey(key)

        val (built, laneInput) = buildEntity(sbn, notification, rankingMap)
        val personKey = Route.personKeyOf(built.shortcutId, built.title)
        val resolution = resolveLane(sbn.packageName, laneInput, personKey)
        val entity = if (resolution == null) built else built.copy(laneId = resolution.laneId)
        val recordId = notificationDao.upsertByKey(entity)
        val updateCount = notificationDao.getById(recordId)?.updateCount ?: 0
        Log.d(TAG, "posted key=$key id=$recordId updateCount=$updateCount lane=${entity.provisionalLane} laneId=${entity.laneId}")

        if (applyRules) applyDismissRules(sbn, entity, recordId)
        if (resolution != null) maybeScreen(entity, resolution, personKey, interactive = applyRules)
    }

    /**
     * Resolves the lane for a post (person > channel > app route > category default).
     * A database failure files the row without a lane rather than dropping the capture.
     */
    private suspend fun resolveLane(packageName: String, input: LaneInput, personKey: String?): Resolution? = try {
        val resolution = LaneResolver.resolve(
            input = input,
            packageName = packageName,
            personKey = personKey,
            routes = laneRepository.getRoutesFor(packageName),
            lanes = laneRepository.getLanes(),
        )
        Log.d(LANES_TAG, "resolved pkg=$packageName lane=${resolution.laneId} source=${resolution.source} screen=${resolution.needsScreening}")
        resolution
    } catch (e: Exception) {
        Log.w(LANES_TAG, "lane resolution failed for pkg=$packageName; filing without lane", e)
        null
    }

    /** Posts the one-time Screener prompt for an unrouted sender (R014). */
    private suspend fun maybeScreen(
        entity: NotificationEntity,
        resolution: Resolution,
        personKey: String?,
        interactive: Boolean,
    ) {
        val screen = ScreenerMapping.shouldScreen(
            needsScreening = resolution.needsScreening,
            packageName = entity.packageName,
            ownPackage = packageName,
            isGroupSummary = entity.isGroupSummary,
            interactive = interactive,
        )
        if (!screen) return
        val target = ScreenerMapping.targetFor(entity.packageName, entity.isConversation, personKey)
        if (screenerNotifier.alreadyPrompted(target)) return
        try {
            screenerNotifier.post(
                target = target,
                appName = entity.appName,
                title = entity.title,
                text = entity.text,
                lanes = laneRepository.getLanes(),
                suggestedLaneId = resolution.laneId,
            )
        } catch (e: Exception) {
            Log.w(LANES_TAG, "screener post failed for ${target.dedupeKey}", e)
        }
    }

    private suspend fun handleRemoved(sbn: StatusBarNotification, reason: Int) {
        val key = sbn.key
        val reasonName = CaptureMapping.removalReasonName(reason)
        pendingIntentCache.remove(key)

        if (liveNotificationDao.deleteByKey(key) > 0) {
            Log.d(TAG, "live removed key=$key reason=$reasonName")
            return
        }
        val id = notificationDao.findIdByKey(key)
        if (id == null) {
            Log.d(TAG, "removed key=$key reason=$reasonName: no live history row")
            return
        }
        notificationDao.markRemoved(id, reasonName, System.currentTimeMillis())
        val updateCount = notificationDao.getById(id)?.updateCount ?: 0
        Log.d(TAG, "removed key=$key id=$id updateCount=$updateCount reason=$reasonName")
    }

    /**
     * Brings the live table in line with what is actually posted and records any
     * non-ongoing notifications posted while the listener was unbound. Rules are not
     * applied here: reconnecting must not suddenly cancel notifications the user can see.
     */
    private suspend fun reconcile() {
        val active = try {
            activeNotifications.orEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "reconcile: activeNotifications unavailable", e)
            return
        }
        val ranking = try { currentRanking } catch (e: Exception) { null }

        val liveKeys = mutableListOf<String>()
        var historyAdded = 0
        for (sbn in active) {
            val notification = sbn.notification ?: continue
            if (isLive(notification)) {
                liveKeys += sbn.key
                cacheIntents(sbn.key, notification)
                liveNotificationDao.upsert(buildLive(sbn, notification))
            } else if (notificationDao.findIdByKey(sbn.key) == null) {
                handlePosted(sbn, ranking, applyRules = false)
                historyAdded++
            } else {
                // Already recorded; only refresh the process-scoped intents.
                cacheIntents(sbn.key, notification)
            }
        }
        val liveRemoved = if (liveKeys.isEmpty()) {
            liveNotificationDao.getAllKeys().size.also { liveNotificationDao.clear() }
        } else {
            liveNotificationDao.deleteNotIn(liveKeys)
        }
        Log.i(
            TAG,
            "reconcile: active=${active.size} live=${liveKeys.size} liveRemoved=$liveRemoved historyAdded=$historyAdded",
        )
    }

    private suspend fun applyDismissRules(sbn: StatusBarNotification, entity: NotificationEntity, recordId: Long) {
        val evalContext = EvaluationContext(
            packageName = sbn.packageName,
            notificationTitle = entity.title,
            notificationText = entity.text,
            notificationPriority = entity.priority,
        )
        val dismissRule = ruleEngine.findMatchingRules(RulePillar.NOTIFICATION, evalContext)
            .firstOrNull { rule -> rule.actions.any { it.type == ActionType.DISMISS_NOTIFICATION } }
            ?: return

        notificationDao.markAutoDismissed(
            id = recordId,
            ruleId = dismissRule.id,
            timestamp = System.currentTimeMillis(),
        )
        Log.d(TAG, "auto-dismiss key=${sbn.key} id=$recordId rule=${dismissRule.id}")
        cancelNotification(sbn.key)
    }

    // -- builders --

    private fun isLive(notification: Notification): Boolean {
        val extras = notification.extras
        return CaptureMapping.isLive(
            flags = notification.flags,
            template = extras?.getString(Notification.EXTRA_TEMPLATE),
            hasMediaSession = extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true,
        )
    }

    private fun cacheIntents(key: String, notification: Notification) {
        pendingIntentCache.put(
            key = key,
            contentIntent = notification.contentIntent,
            actionIntents = notification.actions.orEmpty().map { it?.actionIntent },
        )
    }

    private fun buildLive(sbn: StatusBarNotification, notification: Notification): LiveNotificationEntity {
        val extras = notification.extras
        return LiveNotificationEntity(
            key = sbn.key,
            packageName = sbn.packageName,
            title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            category = notification.category,
            channelId = notification.channelId,
            isOngoing = sbn.isOngoing,
            postTime = sbn.postTime,
            smallIconPath = iconStore.save(sbn.packageName, notification.smallIcon),
            flags = notification.flags,
        )
    }

    private fun buildEntity(
        sbn: StatusBarNotification,
        notification: Notification,
        rankingMap: RankingMap?,
    ): Pair<NotificationEntity, LaneInput> {
        val extras = notification.extras
        val ranking = rankingMap?.let { map -> Ranking().takeIf { map.getRanking(sbn.key, it) } }

        val channelId = ranking?.channel?.id ?: notification.channelId
        val importance = ranking?.importance ?: 0
        val isConversation = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ranking?.isConversation == true
        val shortcutId = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ranking?.conversationShortcutInfo?.id
        } else null) ?: notification.shortcutId

        val title = (extras?.getCharSequence(Notification.EXTRA_TITLE)
            ?: extras?.getCharSequence(Notification.EXTRA_TITLE_BIG))?.toString()
        val text = (extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras?.getCharSequence(Notification.EXTRA_TEXT))?.toString()
        val actions = notification.actions.orEmpty().filterNotNull()
        val hasRemoteInput = actions.any { !it.remoteInputs.isNullOrEmpty() }

        val laneInput = LaneInput(
            category = notification.category,
            channelId = channelId,
            importance = importance,
            isConversation = isConversation,
            isOngoing = sbn.isOngoing,
            hasRemoteInputAction = hasRemoteInput,
        )
        val lane = ProvisionalLaneClassifier.classify(laneInput)

        return NotificationEntity(
            packageName = sbn.packageName,
            appName = AppNameResolver.getAppName(applicationContext, sbn.packageName),
            title = title,
            text = text,
            subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            category = notification.category,
            priority = @Suppress("DEPRECATION") notification.priority,
            timestamp = sbn.postTime,
            groupKey = sbn.groupKey,
            isOngoing = sbn.isOngoing,
            userAction = NotificationAction.NONE.name,
            actionTimestamp = null,
            ruleIdIfAutoDismissed = null,
            key = sbn.key,
            notifId = sbn.id,
            tag = sbn.tag,
            channelId = channelId,
            importance = importance,
            isConversation = isConversation,
            shortcutId = shortcutId,
            whenMs = notification.`when`,
            isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
            flags = notification.flags,
            color = notification.color,
            smallIconPath = iconStore.save(sbn.packageName, notification.smallIcon),
            largeIconPath = iconStore.save(sbn.packageName, notification.getLargeIcon()),
            provisionalLane = lane.name,
            messagesJson = messagesJson(notification),
            actionsJson = actionsJson(actions),
        ) to laneInput
    }

    private fun messagesJson(notification: Notification): String? = try {
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        val messages = style?.messages.orEmpty()
        if (messages.isEmpty()) null else JSONArray().apply {
            messages.forEach { m ->
                put(
                    JSONObject()
                        .put("sender", m.person?.name?.toString())
                        .put("text", m.text?.toString())
                        .put("time", m.timestamp),
                )
            }
        }.toString()
    } catch (e: Exception) {
        Log.w(TAG, "messagesJson: failed to extract messages", e)
        null
    }

    private fun actionsJson(actions: List<Notification.Action>): String? {
        if (actions.isEmpty()) return null
        return JSONArray().apply {
            actions.forEach { a ->
                put(
                    JSONObject()
                        .put("title", a.title?.toString())
                        .put("hasRemoteInput", !a.remoteInputs.isNullOrEmpty())
                        .put("semanticAction", a.semanticAction),
                )
            }
        }.toString()
    }

    companion object {
        private const val TAG = "NotificationCapture"
        private const val LANES_TAG = "Lanes"

        /** The bound listener, or null while unbound; used by [NotificationActionsImpl]. */
        @Volatile
        internal var instance: NotificationCaptureService? = null
            private set
    }
}
