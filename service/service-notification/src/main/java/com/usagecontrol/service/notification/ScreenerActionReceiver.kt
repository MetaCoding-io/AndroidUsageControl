package com.usagecontrol.service.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.usagecontrol.core.database.LaneRepository
import com.usagecontrol.core.model.RouteKind
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles a Screener lane tap: creates the route for the sender and re-files its history
 * into the chosen lane, then clears the prompt. Dependencies come from a Hilt entry point
 * so the receiver needs no generated Hilt_ superclass.
 */
class ScreenerActionReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun laneRepository(): LaneRepository
        fun screenerNotifier(): ScreenerNotifier
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ROUTE) return
        val kind = RouteKind.fromName(intent.getStringExtra(EXTRA_KIND))
        val packageName = intent.getStringExtra(EXTRA_PACKAGE)
        val key = intent.getStringExtra(EXTRA_KEY).orEmpty()
        val laneId = intent.getLongExtra(EXTRA_LANE_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        if (kind == null || packageName.isNullOrEmpty() || laneId <= 0 || (kind != RouteKind.APP && key.isEmpty())) {
            Log.w(TAG, "screener route ignored: malformed extras kind=$kind pkg=$packageName key=$key lane=$laneId")
            return
        }

        val deps = EntryPointAccessors.fromApplication(context.applicationContext, ReceiverEntryPoint::class.java)
        val pending = goAsync()
        scope.launch {
            try {
                val reFiled = deps.laneRepository().setRoute(kind, packageName, key, laneId)
                Log.i(TAG, "screener routed kind=$kind pkg=$packageName key=$key lane=$laneId reFiled=$reFiled")
            } catch (e: Exception) {
                Log.e(TAG, "screener route failed kind=$kind pkg=$packageName lane=$laneId", e)
            } finally {
                if (notificationId != 0) deps.screenerNotifier().cancel(notificationId)
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "Lanes"
        const val ACTION_ROUTE = "com.usagecontrol.action.SCREENER_ROUTE"
        const val EXTRA_KIND = "kind"
        const val EXTRA_PACKAGE = "packageName"
        const val EXTRA_KEY = "key"
        const val EXTRA_LANE_ID = "laneId"
        const val EXTRA_NOTIFICATION_ID = "notificationId"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun routeIntent(context: Context, target: ScreenerTarget, laneId: Long, notificationId: Int): Intent =
            Intent(context, ScreenerActionReceiver::class.java)
                .setAction(ACTION_ROUTE)
                .putExtra(EXTRA_KIND, target.kind.name)
                .putExtra(EXTRA_PACKAGE, target.packageName)
                .putExtra(EXTRA_KEY, target.key)
                .putExtra(EXTRA_LANE_ID, laneId)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId)
    }
}
