package com.usagecontrol.service.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.usagecontrol.core.common.AppNameResolver
import com.usagecontrol.core.database.dao.NotificationDao
import com.usagecontrol.core.database.entity.NotificationEntity
import com.usagecontrol.core.database.toModel
import com.usagecontrol.core.model.NotificationAction
import com.usagecontrol.core.model.RulePillar
import com.usagecontrol.core.rules.EvaluationContext
import com.usagecontrol.core.rules.RuleEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationCaptureService : NotificationListenerService() {

    @Inject lateinit var notificationDao: NotificationDao
    @Inject lateinit var ruleEngine: RuleEngine

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        scope.launch {
            val notification = sbn.notification ?: return@launch
            val extras = notification.extras

            val entity = NotificationEntity(
                packageName = sbn.packageName,
                appName = AppNameResolver.getAppName(applicationContext, sbn.packageName),
                title = extras?.getCharSequence("android.title")?.toString(),
                text = extras?.getCharSequence("android.text")?.toString(),
                subText = extras?.getCharSequence("android.subText")?.toString(),
                category = notification.category,
                priority = notification.priority,
                timestamp = sbn.postTime,
                groupKey = sbn.groupKey,
                isOngoing = sbn.isOngoing,
                userAction = NotificationAction.NONE.name,
                actionTimestamp = null,
                ruleIdIfAutoDismissed = null,
            )

            val recordId = notificationDao.insert(entity)

            // Check rules for auto-dismiss
            val evalContext = EvaluationContext(
                packageName = sbn.packageName,
                notificationTitle = entity.title,
                notificationText = entity.text,
                notificationPriority = notification.priority,
            )

            val matchingRules = ruleEngine.findMatchingRules(RulePillar.NOTIFICATION, evalContext)
            val dismissRule = matchingRules.firstOrNull { rule ->
                rule.actions.any { it.type == com.usagecontrol.core.model.ActionType.DISMISS_NOTIFICATION }
            }

            if (dismissRule != null) {
                notificationDao.markAutoDismissed(
                    id = recordId,
                    ruleId = dismissRule.id,
                    timestamp = System.currentTimeMillis(),
                )
                cancelNotification(sbn.key)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap?, reason: Int) {
        scope.launch {
            // Try to find the most recent notification from this package at this time
            // and mark it as dismissed (if the user dismissed it, not auto-dismissed)
            val recent = notificationDao.search(
                packageName = sbn.packageName,
                searchQuery = null,
                startTime = sbn.postTime - 1000,
                endTime = sbn.postTime + 1000,
                limit = 1,
            ).firstOrNull()

            if (recent != null && recent.userAction == NotificationAction.NONE.name) {
                val action = when (reason) {
                    REASON_CLICK -> NotificationAction.OPENED
                    REASON_CANCEL, REASON_CANCEL_ALL -> NotificationAction.DISMISSED
                    else -> NotificationAction.DISMISSED
                }
                notificationDao.updateAction(recent.id, action.name, System.currentTimeMillis())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
