package com.usagecontrol.core.common.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/** Single DataStore delegate -- MUST be provided as @Singleton via Hilt to prevent multiple instances */
val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "onboarding_prefs"
)

object OnboardingPreferencesKeys {
    val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
}
