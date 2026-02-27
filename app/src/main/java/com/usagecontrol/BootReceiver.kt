package com.usagecontrol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.usagecontrol.service.context.ContextMonitorService
import com.usagecontrol.service.usage.UsageMonitorService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Restart monitoring services after device reboot
            context.startForegroundService(Intent(context, UsageMonitorService::class.java))
            context.startForegroundService(Intent(context, ContextMonitorService::class.java))
        }
    }
}
