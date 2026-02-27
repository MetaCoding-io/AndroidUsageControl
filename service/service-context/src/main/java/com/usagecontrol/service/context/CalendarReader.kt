package com.usagecontrol.service.context

import android.content.ContentResolver
import android.content.Context
import android.provider.CalendarContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the current calendar event from the device's calendar provider.
 * Returns the event title if there is an active event right now.
 */
@Singleton
class CalendarReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Query the calendar for events happening right now.
     * Returns the title of the first matching event, or null if none.
     */
    fun getCurrentEvent(): String? {
        return try {
            val resolver: ContentResolver = context.contentResolver
            val now = System.currentTimeMillis()

            val projection = arrayOf(
                CalendarContract.Events.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
            )

            // Query instances (events expanded by recurrence) in a window around now
            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                .appendPath(now.toString())
                .appendPath(now.toString())
                .build()

            resolver.query(
                uri,
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                    if (titleIdx >= 0) cursor.getString(titleIdx) else null
                } else {
                    null
                }
            }
        } catch (_: SecurityException) {
            // Calendar permission not granted
            null
        } catch (_: Exception) {
            null
        }
    }
}
