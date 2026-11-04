package com.usagecontrol.service.notification

import android.app.PendingIntent
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-scoped cache of notification PendingIntents keyed by StatusBarNotification key.
 * PendingIntents cannot be persisted, so entries vanish with the process and are
 * cleared when the listener disconnects.
 */
@Singleton
class PendingIntentCache @Inject constructor() {

    data class Entry(
        val contentIntent: PendingIntent?,
        /** Action intents in the notification's action order; null where an action has none. */
        val actionIntents: List<PendingIntent?> = emptyList(),
    )

    private val entries = ConcurrentHashMap<String, Entry>()

    fun put(key: String, contentIntent: PendingIntent?, actionIntents: List<PendingIntent?> = emptyList()) {
        if (contentIntent == null && actionIntents.all { it == null }) {
            entries.remove(key)
        } else {
            entries[key] = Entry(contentIntent, actionIntents)
        }
    }

    fun get(key: String): Entry? = entries[key]

    fun contentIntent(key: String): PendingIntent? = entries[key]?.contentIntent

    fun actionIntent(key: String, index: Int): PendingIntent? =
        entries[key]?.actionIntents?.getOrNull(index)

    fun remove(key: String): Entry? = entries.remove(key)

    fun clear() = entries.clear()

    val size: Int get() = entries.size
}
