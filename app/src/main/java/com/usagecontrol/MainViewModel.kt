package com.usagecontrol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagecontrol.core.common.PermissionChecker
import com.usagecontrol.core.common.onboarding.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val permissionChecker: PermissionChecker,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination

    init {
        viewModelScope.launch {
            val onboardingComplete = onboardingRepository.isOnboardingComplete.first()
            if (onboardingComplete) {
                _startDestination.value = "main"
            } else {
                // ONBD-04: v0.1 upgrader auto-skip -- if all permissions already granted,
                // user had v0.1 installed, skip onboarding entirely
                if (permissionChecker.allPermissionsGranted()) {
                    onboardingRepository.setOnboardingComplete(true)
                    _startDestination.value = "main"
                } else {
                    _startDestination.value = "onboarding"
                }
            }
        }
    }

    /** Called when user completes or skips onboarding. Writes DataStore flag and returns. */
    fun markOnboardingComplete() {
        viewModelScope.launch {
            onboardingRepository.setOnboardingComplete(true)
        }
    }
}
