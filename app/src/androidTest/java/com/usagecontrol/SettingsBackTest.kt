package com.usagecontrol

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.usagecontrol.core.common.onboarding.OnboardingPreferencesKeys
import com.usagecontrol.core.common.onboarding.onboardingDataStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Settings is a pushed screen with the shell top bar hidden, so its own back arrow is the
 * only on-screen way back to the tabs.
 */
@RunWith(AndroidJUnit4::class)
class SettingsBackTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        // Skip onboarding so the app starts on the Now tab.
        runBlocking {
            context.onboardingDataStore.edit { it[OnboardingPreferencesKeys.ONBOARDING_COMPLETE] = true }
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun backArrow_returnsFromSettingsToNowTab() {
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithTag("settings_gear").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_gear").performClick()

        composeRule.onNodeWithTag("settings_back").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_gear").assertDoesNotExist()

        composeRule.onNodeWithTag("settings_back").performClick()

        composeRule.onNodeWithTag("settings_back").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_gear").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_now").assertIsDisplayed().assertIsSelected()
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
