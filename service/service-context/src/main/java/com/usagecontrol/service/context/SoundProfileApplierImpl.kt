package com.usagecontrol.service.context

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import com.usagecontrol.core.common.SoundProfileApplier
import com.usagecontrol.core.common.SoundProfileApplier.ApplyResult
import com.usagecontrol.core.model.RingerMode
import com.usagecontrol.core.model.SoundPreset
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SoundProfileApplier] backed by [SoundProfileExecutor]. Every apply is logged under the
 * SoundProfileApplier tag with the preset id and its outcome (applied, permission-denied, error).
 *
 * [SoundProfileExecutor] swallows SecurityException per setting, so DND access is checked up
 * front: toggling DND, or entering SILENT (which Android treats as a DND change), needs
 * notification policy access.
 */
@Singleton
class SoundProfileApplierImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val soundProfileExecutor: SoundProfileExecutor,
) : SoundProfileApplier {

    override suspend fun apply(preset: SoundPreset): ApplyResult = withContext(Dispatchers.Default) {
        val result = try {
            if (needsPolicyAccess(preset) && !hasPolicyAccess()) {
                ApplyResult.PermissionDenied
            } else {
                soundProfileExecutor.applyPreset(preset)
                ApplyResult.Applied
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "preset=${preset.id} security exception", e)
            ApplyResult.PermissionDenied
        } catch (e: Exception) {
            Log.e(TAG, "preset=${preset.id} failed", e)
            ApplyResult.Error(e.message ?: e.javaClass.simpleName)
        }
        Log.i(TAG, "preset=${preset.id} outcome=${result.label()}")
        result
    }

    private fun needsPolicyAccess(preset: SoundPreset): Boolean =
        preset.dndEnabled != null || preset.ringerMode == RingerMode.SILENT

    private fun hasPolicyAccess(): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    private fun ApplyResult.label(): String = when (this) {
        ApplyResult.Applied -> "applied"
        ApplyResult.PermissionDenied -> "permission-denied"
        is ApplyResult.Error -> "error"
    }

    private companion object {
        const val TAG = "SoundProfileApplier"
    }
}
