package com.usagecontrol.feature.onboarding

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagecontrol.core.common.PermissionChecker
import com.usagecontrol.core.common.onboarding.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PermissionWalkthroughState(
    val currentStep: Int = 0,
    val permissionStates: List<Boolean> = List(PermissionStep.TOTAL_STEPS) { false },
    val isComplete: Boolean = false,
)

@HiltViewModel
class PermissionWalkthroughViewModel @Inject constructor(
    private val permissionChecker: PermissionChecker,
    private val onboardingRepository: OnboardingRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(PermissionWalkthroughState())
    val state: StateFlow<PermissionWalkthroughState> = _state.asStateFlow()

    init {
        recheckAll()
        // Auto-advance past already-granted steps (handles app kill + relaunch mid-walkthrough)
        autoAdvancePastGranted()
    }

    /** Called from ON_RESUME observer -- re-reads all three permissions. */
    fun recheckAll() {
        val wasNotificationGranted = _state.value.permissionStates[PermissionStep.NotificationAccess.index]
        val isNowNotificationGranted = permissionChecker.isNotificationAccessGranted()

        // Pitfall 5: NLS rebind after Notification Access grant transition
        if (!wasNotificationGranted && isNowNotificationGranted) {
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(
                        context.packageName,
                        "com.usagecontrol.service.notification.NotificationCaptureService",
                    )
                )
            } catch (_: Exception) {
                // Safe to ignore -- requestRebind may fail if component not declared
            }
        }

        _state.update { current ->
            current.copy(
                permissionStates = listOf(
                    isNowNotificationGranted,
                    permissionChecker.isUsageAccessGranted(),
                    permissionChecker.isDndAccessGranted(),
                )
            )
        }
    }

    /** Advance to next step (after grant or skip). */
    fun advanceStep() {
        val nextStep = _state.value.currentStep + 1
        if (nextStep >= PermissionStep.TOTAL_STEPS) {
            completeOnboarding()
        } else {
            _state.update { it.copy(currentStep = nextStep) }
        }
    }

    private fun autoAdvancePastGranted() {
        // Skip already-granted steps on init (e.g., app killed and relaunched mid-walkthrough)
        val states = _state.value.permissionStates
        var step = _state.value.currentStep
        while (step < PermissionStep.TOTAL_STEPS && states[step]) {
            step++
        }
        if (step >= PermissionStep.TOTAL_STEPS) {
            completeOnboarding()
        } else if (step != _state.value.currentStep) {
            _state.update { it.copy(currentStep = step) }
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            onboardingRepository.setOnboardingComplete(true)
            _state.update { it.copy(isComplete = true) }
        }
    }
}
