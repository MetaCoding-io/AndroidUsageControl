package com.usagecontrol.service.context

import com.usagecontrol.core.common.SoundProfileApplier
import com.usagecontrol.core.model.ActionType
import com.usagecontrol.core.rules.ActionExecutor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers LOCK_SOUND action handlers into the shared ActionExecutor.
 * Called once during service initialization to wire up the sound and lock executors.
 */
@Singleton
class ActionHandlerRegistrar @Inject constructor(
    private val actionExecutor: ActionExecutor,
    private val soundProfileExecutor: SoundProfileExecutor,
    private val lockControlExecutor: LockControlExecutor,
) {

    private var initialized = false

    fun ensureRegistered() {
        if (initialized) return
        initialized = true

        actionExecutor.registerHandler(ActionType.SET_VOLUME) { action, _ ->
            val stream = action.params["stream"] ?: return@registerHandler
            val level = action.params["level"]?.toIntOrNull() ?: return@registerHandler
            soundProfileExecutor.setStreamVolumeByName(stream, level)
        }

        actionExecutor.registerHandler(ActionType.SET_RINGER_MODE) { action, _ ->
            val mode = action.params["mode"] ?: return@registerHandler
            soundProfileExecutor.setRingerModeFromString(mode)
        }

        actionExecutor.registerHandler(ActionType.SET_DND) { action, _ ->
            val enabled = action.params["enabled"]?.toBoolean() ?: return@registerHandler
            soundProfileExecutor.setDnd(enabled)
        }

        actionExecutor.registerHandler(ActionType.SET_LOCK_TIMEOUT) { action, _ ->
            val timeoutMs = action.params["timeoutMs"]?.toLongOrNull() ?: return@registerHandler
            lockControlExecutor.setLockTimeout(timeoutMs)
        }
    }
}

/** Binds the Modes-tab [SoundProfileApplier] bridge to its service-context implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SoundProfileApplierModule {
    @Binds
    abstract fun bindSoundProfileApplier(impl: SoundProfileApplierImpl): SoundProfileApplier
}
