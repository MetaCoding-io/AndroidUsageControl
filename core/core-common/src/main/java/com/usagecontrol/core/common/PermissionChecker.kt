package com.usagecontrol.core.common

import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.os.Process
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Notification Access -- checks Settings.Secure enabled_notification_listeners */
    fun isNotificationAccessGranted(): Boolean {
        return NotificationManagerCompat
            .getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    /** Usage Access -- uses AppOpsManager, NOT checkSelfPermission (always returns DENIED for this) */
    fun isUsageAccessGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** DND Access -- uses NotificationManager.isNotificationPolicyAccessGranted */
    fun isDndAccessGranted(): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java)
        return nm.isNotificationPolicyAccessGranted
    }

    /** All three required permissions granted */
    fun allPermissionsGranted(): Boolean =
        isNotificationAccessGranted() && isUsageAccessGranted() && isDndAccessGranted()
}
