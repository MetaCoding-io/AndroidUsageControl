package com.usagecontrol.service.context

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Device Admin receiver for lock timeout control.
 * Requires user to explicitly grant device admin privileges in Settings.
 */
class DeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        // Device admin enabled
    }

    override fun onDisabled(context: Context, intent: Intent) {
        // Device admin disabled
    }
}
