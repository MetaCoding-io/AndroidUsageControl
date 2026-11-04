package com.usagecontrol.core.rules

import com.usagecontrol.core.model.ProvisionalLane
import com.usagecontrol.core.rules.ProvisionalLaneClassifier.CATEGORY_ALARM
import com.usagecontrol.core.rules.ProvisionalLaneClassifier.CATEGORY_CALL
import com.usagecontrol.core.rules.ProvisionalLaneClassifier.CATEGORY_MESSAGE
import com.usagecontrol.core.rules.ProvisionalLaneClassifier.CATEGORY_PROMO
import com.usagecontrol.core.rules.ProvisionalLaneClassifier.CATEGORY_RECOMMENDATION
import com.usagecontrol.core.rules.ProvisionalLaneClassifier.CATEGORY_REMINDER
import com.usagecontrol.core.rules.ProvisionalLaneClassifier.CATEGORY_SOCIAL
import com.usagecontrol.core.rules.ProvisionalLaneClassifier.CHANNEL_NEWS
import com.usagecontrol.core.rules.ProvisionalLaneClassifier.CHANNEL_PROMOTIONS
import com.usagecontrol.core.rules.ProvisionalLaneClassifier.CHANNEL_RECS
import com.usagecontrol.core.rules.ProvisionalLaneClassifier.CHANNEL_SOCIAL_MEDIA
import com.usagecontrol.core.rules.ProvisionalLaneClassifier.IMPORTANCE_DEFAULT
import com.usagecontrol.core.rules.ProvisionalLaneClassifier.IMPORTANCE_HIGH
import com.usagecontrol.core.rules.ProvisionalLaneClassifier.IMPORTANCE_LOW
import com.usagecontrol.core.rules.ProvisionalLaneClassifier.IMPORTANCE_MIN
import com.usagecontrol.core.rules.ProvisionalLaneClassifier.IMPORTANCE_NONE
import com.usagecontrol.core.rules.ProvisionalLaneClassifier.IMPORTANCE_UNSPECIFIED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProvisionalLaneClassifierTest {

    private fun lane(input: LaneInput) = ProvisionalLaneClassifier.classify(input)

    // PEOPLE

    @Test fun conversationIsPeople() =
        assertEquals(ProvisionalLane.PEOPLE, lane(LaneInput(isConversation = true)))

    @Test fun messageCategoryIsPeople() =
        assertEquals(ProvisionalLane.PEOPLE, lane(LaneInput(category = CATEGORY_MESSAGE)))

    @Test fun callCategoryIsPeople() =
        assertEquals(ProvisionalLane.PEOPLE, lane(LaneInput(category = CATEGORY_CALL)))

    @Test fun remoteInputIsPeople() =
        assertEquals(ProvisionalLane.PEOPLE, lane(LaneInput(hasRemoteInputAction = true)))

    @Test fun ongoingRemoteInputIsNotPeople() =
        assertEquals(ProvisionalLane.UPDATES, lane(LaneInput(hasRemoteInputAction = true, isOngoing = true)))

    @Test fun conversationBeatsLowImportanceAndPromoChannel() =
        assertEquals(
            ProvisionalLane.PEOPLE,
            lane(LaneInput(isConversation = true, importance = IMPORTANCE_MIN, channelId = CHANNEL_PROMOTIONS)),
        )

    @Test fun categoryMatchIsCaseInsensitive() =
        assertEquals(ProvisionalLane.PEOPLE, lane(LaneInput(category = "MSG")))

    // PRIORITY

    @Test fun alarmIsPriority() =
        assertEquals(ProvisionalLane.PRIORITY, lane(LaneInput(category = CATEGORY_ALARM)))

    @Test fun reminderIsPriority() =
        assertEquals(ProvisionalLane.PRIORITY, lane(LaneInput(category = CATEGORY_REMINDER)))

    @Test fun highImportanceIsPriority() =
        assertEquals(ProvisionalLane.PRIORITY, lane(LaneInput(importance = IMPORTANCE_HIGH)))

    @Test fun maxImportanceIsPriority() =
        assertEquals(ProvisionalLane.PRIORITY, lane(LaneInput(importance = 5)))

    @Test fun ongoingHighImportanceIsNotPriority() =
        assertEquals(ProvisionalLane.UPDATES, lane(LaneInput(importance = IMPORTANCE_HIGH, isOngoing = true)))

    @Test fun ongoingAlarmStaysPriority() =
        assertEquals(ProvisionalLane.PRIORITY, lane(LaneInput(category = CATEGORY_ALARM, isOngoing = true)))

    @Test fun alarmBeatsLowImportance() =
        assertEquals(ProvisionalLane.PRIORITY, lane(LaneInput(category = CATEGORY_ALARM, importance = IMPORTANCE_LOW)))

    // QUIET

    @Test fun promoIsQuiet() =
        assertEquals(ProvisionalLane.QUIET, lane(LaneInput(category = CATEGORY_PROMO)))

    @Test fun socialIsQuiet() =
        assertEquals(ProvisionalLane.QUIET, lane(LaneInput(category = CATEGORY_SOCIAL)))

    @Test fun recommendationIsQuiet() =
        assertEquals(ProvisionalLane.QUIET, lane(LaneInput(category = CATEGORY_RECOMMENDATION)))

    @Test fun lowImportanceIsQuiet() =
        assertEquals(ProvisionalLane.QUIET, lane(LaneInput(importance = IMPORTANCE_LOW)))

    @Test fun minImportanceIsQuiet() =
        assertEquals(ProvisionalLane.QUIET, lane(LaneInput(importance = IMPORTANCE_MIN)))

    @Test fun noneImportanceIsQuiet() =
        assertEquals(ProvisionalLane.QUIET, lane(LaneInput(importance = IMPORTANCE_NONE)))

    @Test fun highImportancePromoIsPriority() =
        assertEquals(ProvisionalLane.PRIORITY, lane(LaneInput(category = CATEGORY_PROMO, importance = IMPORTANCE_HIGH)))

    // Bundle channels

    @Test fun promotionsChannelIsQuiet() =
        assertEquals(ProvisionalLane.QUIET, lane(LaneInput(channelId = CHANNEL_PROMOTIONS)))

    @Test fun socialChannelIsQuiet() =
        assertEquals(ProvisionalLane.QUIET, lane(LaneInput(channelId = CHANNEL_SOCIAL_MEDIA)))

    @Test fun recsChannelIsQuiet() =
        assertEquals(ProvisionalLane.QUIET, lane(LaneInput(channelId = CHANNEL_RECS)))

    @Test fun newsChannelIsUpdatesEvenAtLowImportance() =
        assertEquals(ProvisionalLane.UPDATES, lane(LaneInput(channelId = CHANNEL_NEWS, importance = IMPORTANCE_LOW)))

    @Test fun bundleChannelBeatsHighImportance() =
        assertEquals(ProvisionalLane.QUIET, lane(LaneInput(channelId = CHANNEL_PROMOTIONS, importance = IMPORTANCE_HIGH)))

    // UPDATES / boundaries

    @Test fun defaultIsUpdates() =
        assertEquals(ProvisionalLane.UPDATES, lane(LaneInput()))

    @Test fun defaultImportanceIsUpdates() =
        assertEquals(ProvisionalLane.UPDATES, lane(LaneInput(importance = IMPORTANCE_DEFAULT)))

    @Test fun unspecifiedImportanceIsUpdates() =
        assertEquals(ProvisionalLane.UPDATES, lane(LaneInput(importance = IMPORTANCE_UNSPECIFIED)))

    @Test fun unknownCategoryAndChannelAreUpdates() =
        assertEquals(ProvisionalLane.UPDATES, lane(LaneInput(category = "bogus", channelId = "android.app.unknown")))

    @Test fun emptyCategoryIsUpdates() =
        assertEquals(ProvisionalLane.UPDATES, lane(LaneInput(category = "", channelId = "")))

    // Persistence round-trip

    @Test fun fromNameRoundTrips() =
        ProvisionalLane.entries.forEach { assertEquals(it, ProvisionalLane.fromName(it.name)) }

    @Test fun fromNameRejectsUnknown() {
        assertNull(ProvisionalLane.fromName(null))
        assertNull(ProvisionalLane.fromName("NOPE"))
    }
}
