package com.usagecontrol.core.common.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class OnboardingRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val isOnboardingComplete: Flow<Boolean> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }
        .map { prefs ->
            prefs[OnboardingPreferencesKeys.ONBOARDING_COMPLETE] ?: false
        }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { prefs ->
            prefs[OnboardingPreferencesKeys.ONBOARDING_COMPLETE] = complete
        }
    }
}
