package com.usagecontrol.core.rules

import com.usagecontrol.core.model.ProvisionalLane

/** Capture-time signals used to pick a provisional lane. */
data class LaneInput(
    val category: String? = null,
    val channelId: String? = null,
    val importance: Int = ProvisionalLaneClassifier.IMPORTANCE_DEFAULT,
    val isConversation: Boolean = false,
    val isOngoing: Boolean = false,
    val hasRemoteInputAction: Boolean = false,
)

/**
 * Pure classifier mapping notification metadata to a [ProvisionalLane].
 *
 * Android constants are mirrored as literals so the rules stay JVM-testable and
 * don't depend on compileSdk exposing newer symbols (e.g. Android 16 bundle channels).
 *
 * Precedence: PEOPLE > reserved bundle channel > PRIORITY > QUIET > UPDATES.
 */
object ProvisionalLaneClassifier {

    // NotificationManager.IMPORTANCE_*
    const val IMPORTANCE_NONE = 0
    const val IMPORTANCE_MIN = 1
    const val IMPORTANCE_LOW = 2
    const val IMPORTANCE_DEFAULT = 3
    const val IMPORTANCE_HIGH = 4
    const val IMPORTANCE_UNSPECIFIED = -1000

    // Notification.CATEGORY_*
    const val CATEGORY_MESSAGE = "msg"
    const val CATEGORY_CALL = "call"
    const val CATEGORY_ALARM = "alarm"
    const val CATEGORY_REMINDER = "reminder"
    const val CATEGORY_PROMO = "promo"
    const val CATEGORY_SOCIAL = "social"
    const val CATEGORY_RECOMMENDATION = "recommendation"

    // Android 16 reserved bundle channel ids (NotificationChannel.*_ID)
    const val CHANNEL_PROMOTIONS = "android.app.promotions"
    const val CHANNEL_SOCIAL_MEDIA = "android.app.social"
    const val CHANNEL_NEWS = "android.app.news"
    const val CHANNEL_RECS = "android.app.recs"

    private val peopleCategories = setOf(CATEGORY_MESSAGE, CATEGORY_CALL)
    private val priorityCategories = setOf(CATEGORY_ALARM, CATEGORY_REMINDER)
    private val quietCategories = setOf(CATEGORY_PROMO, CATEGORY_SOCIAL, CATEGORY_RECOMMENDATION)

    private val bundleChannels = mapOf(
        CHANNEL_PROMOTIONS to ProvisionalLane.QUIET,
        CHANNEL_SOCIAL_MEDIA to ProvisionalLane.QUIET,
        CHANNEL_RECS to ProvisionalLane.QUIET,
        CHANNEL_NEWS to ProvisionalLane.UPDATES,
    )

    fun classify(input: LaneInput): ProvisionalLane {
        val category = input.category?.lowercase()
        val importance = input.importance

        if (input.isConversation || category in peopleCategories) return ProvisionalLane.PEOPLE
        // A reply action on a non-ongoing notification is a strong conversation signal.
        if (input.hasRemoteInputAction && !input.isOngoing) return ProvisionalLane.PEOPLE

        bundleChannels[input.channelId]?.let { return it }

        if (category in priorityCategories) return ProvisionalLane.PRIORITY
        // Ongoing notifications (navigation, timers) use HIGH importance to stay visible,
        // not to interrupt, so importance alone doesn't promote them.
        if (importance >= IMPORTANCE_HIGH && !input.isOngoing) return ProvisionalLane.PRIORITY

        if (category in quietCategories) return ProvisionalLane.QUIET
        if (importance != IMPORTANCE_UNSPECIFIED && importance <= IMPORTANCE_LOW) return ProvisionalLane.QUIET

        return ProvisionalLane.UPDATES
    }
}
