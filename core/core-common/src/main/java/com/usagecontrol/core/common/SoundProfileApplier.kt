package com.usagecontrol.core.common

import com.usagecontrol.core.model.SoundPreset

/**
 * Applies a sound preset to the device on the user's behalf (Modes tab toggle).
 * Lives in core-common so feature modules can apply presets without depending on
 * service modules; the implementation is bound by service-context.
 */
interface SoundProfileApplier {

    /** Outcome of [apply]. */
    sealed interface ApplyResult {
        /** Every setting in the preset was handed to the system. */
        data object Applied : ApplyResult

        /** The preset needs Do Not Disturb access (notification policy) that is not granted. */
        data object PermissionDenied : ApplyResult

        /** The system rejected the change for another reason. */
        data class Error(val message: String) : ApplyResult
    }

    /** Applies ringer mode, stream volumes and DND from [preset]. */
    suspend fun apply(preset: SoundPreset): ApplyResult
}
