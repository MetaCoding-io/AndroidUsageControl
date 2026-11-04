package com.usagecontrol.service.notification

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.usagecontrol.core.common.NotificationActions
import com.usagecontrol.core.common.NotificationActions.OpenResult
import com.usagecontrol.core.common.NotificationActions.Result
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [NotificationActions] backed by the bound [NotificationCaptureService] and the
 * process-scoped [PendingIntentCache]. Every action is logged under the NotificationCapture
 * tag with its key and outcome.
 */
@Singleton
class NotificationActionsImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pendingIntentCache: PendingIntentCache,
) : NotificationActions {

    override val isListenerConnected: Boolean
        get() = NotificationCaptureService.instance != null

    override fun archive(key: String): Result {
        val service = NotificationCaptureService.instance
            ?: return Result.LISTENER_UNAVAILABLE.also { Log.w(TAG, "archive key=$key: listener-unavailable") }
        return try {
            service.cancelNotification(key)
            Log.i(TAG, "archive key=$key: cancelled")
            Result.DONE
        } catch (e: Exception) {
            Log.w(TAG, "archive key=$key: failed", e)
            Result.FAILED
        }
    }

    override fun snooze(key: String, durationMs: Long): Result {
        val service = NotificationCaptureService.instance
            ?: return Result.LISTENER_UNAVAILABLE.also { Log.w(TAG, "snooze key=$key: listener-unavailable") }
        if (durationMs <= 0) {
            Log.w(TAG, "snooze key=$key: failed (non-positive duration $durationMs)")
            return Result.FAILED
        }
        return try {
            service.snoozeNotification(key, durationMs)
            Log.i(TAG, "snooze key=$key durationMs=$durationMs: snoozed")
            Result.DONE
        } catch (e: Exception) {
            Log.w(TAG, "snooze key=$key: failed", e)
            Result.FAILED
        }
    }

    override fun open(key: String?, packageName: String): OpenResult {
        val intent = key?.let { pendingIntentCache.contentIntent(it) }
        if (intent != null) {
            try {
                intent.send(context, 0, null, null, null, null, sendOptions())
                Log.i(TAG, "open key=$key: opened")
                return OpenResult.OPENED
            } catch (e: PendingIntent.CanceledException) {
                Log.w(TAG, "open key=$key: content intent cancelled")
                pendingIntentCache.remove(key)
            } catch (e: Exception) {
                Log.w(TAG, "open key=$key: content intent failed", e)
            }
        }
        val launch = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launch == null) {
            Log.w(TAG, "open key=$key pkg=$packageName: intent-missing, no launch intent")
            return OpenResult.UNAVAILABLE
        }
        return try {
            context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Log.i(TAG, "open key=$key pkg=$packageName: intent-missing→launch-fallback")
            OpenResult.FALLBACK_LAUNCHED
        } catch (e: Exception) {
            Log.w(TAG, "open key=$key pkg=$packageName: launch fallback failed", e)
            OpenResult.UNAVAILABLE
        }
    }

    /**
     * From Android 14 a sender must opt in before its foreground state lets the creator's
     * activity start; Now is visible when the user taps, so allow it.
     */
    private fun sendOptions(): Bundle? = when {
        Build.VERSION.SDK_INT >= 36 -> ActivityOptions.makeBasic()
            .setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE,
            ).toBundle()
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> ActivityOptions.makeBasic()
            .setPendingIntentBackgroundActivityStartMode(
                @Suppress("DEPRECATION") ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
            ).toBundle()
        else -> null
    }

    private companion object {
        const val TAG = "NotificationCapture"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationActionsModule {
    @Binds
    abstract fun bindNotificationActions(impl: NotificationActionsImpl): NotificationActions
}
