package com.usagecontrol.service.context

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles lock-related device control.
 * Uses DevicePolicyManager to adjust lock timeout when Device Admin is enabled.
 */
@Singleton
class LockControlExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val devicePolicyManager: DevicePolicyManager
        get() = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    /**
     * Set the maximum time to lock after screen off.
     * Requires Device Admin privilege.
     * @param timeoutMs Lock timeout in milliseconds (0 = immediate lock)
     */
    fun setLockTimeout(timeoutMs: Long) {
        try {
            val adminComponent = ComponentName(context, DeviceAdminReceiver::class.java)
            if (devicePolicyManager.isAdminActive(adminComponent)) {
                devicePolicyManager.setMaximumTimeToLock(adminComponent, timeoutMs)
            }
        } catch (_: SecurityException) {
            // Not a device admin
        }
    }

    /**
     * Check if device admin is active.
     */
    fun isDeviceAdminActive(): Boolean {
        return try {
            val adminComponent = ComponentName(context, DeviceAdminReceiver::class.java)
            devicePolicyManager.isAdminActive(adminComponent)
        } catch (_: Exception) {
            false
        }
    }
}
