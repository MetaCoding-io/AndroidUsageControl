package com.usagecontrol.feature.notifications

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.usagecontrol.core.model.NotificationRecord
import com.usagecontrol.core.ui.theme.DoormanTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression: the swipe state is saveable, so a row restored by Undo came back already
 * dismissed and, in the app, archived itself again.
 */
@RunWith(AndroidJUnit4::class)
class SwipeArchiveUndoTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val record = NotificationRecord(
        id = 7,
        packageName = "com.example",
        appName = "Example",
        title = "Hello",
        text = "World",
        subText = null,
        category = null,
        priority = 0,
        timestamp = System.currentTimeMillis(),
        groupKey = null,
        isOngoing = false,
    )

    @Test
    fun rowRestoredByUndo_isSettledAndNotArchivedAgain() {
        val rows = mutableStateListOf(record)
        var archives = 0
        var swipeState: SwipeToDismissBoxState? = null
        val actions = RowActions(onOpen = {}, onArchive = { archives++ }, onSnoozeRequest = {})
        composeRule.setContent {
            DoormanTheme {
                LazyColumn {
                    items(rows, key = { it.id }) {
                        val state = rememberSwipeToDismissBoxState()
                        swipeState = state
                        SwipeableNotificationItem(it, actions, swipeState = state)
                    }
                }
            }
        }

        composeRule.onNodeWithTag("now_swipe").performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        assertEquals(1, archives)

        // NowViewModel hides the row through the DB flow once the swipe settles; Undo brings
        // the same record back under the same key.
        composeRule.runOnIdle { rows.remove(record) }
        composeRule.waitForIdle()
        composeRule.runOnIdle { rows.add(record) }
        composeRule.waitForIdle()

        assertEquals(SwipeToDismissBoxValue.Settled, swipeState?.currentValue)
        assertEquals("Undo must not trigger a second archive", 1, archives)
    }
}
