package com.usagecontrol.core.rules

import com.usagecontrol.core.model.DefaultLanes
import com.usagecontrol.core.model.Lane
import com.usagecontrol.core.model.LanePolicy

/** Per-app dismissal behaviour over the analysis window. */
data class AppDismissalStats(
    val packageName: String,
    val appName: String,
    val total: Int,
    /** Notifications the user dismissed within [ReScreenPromptEngine.QUICK_DISMISS_MS] of posting. */
    val quickDismissed: Int,
    /** Lane the app is currently routed to (APP route or its usual default), null if unknown. */
    val currentLaneId: Long? = null,
)

/** A habit-based suggestion to re-route an app, phrased as a question (not a rule). */
data class ReScreenPrompt(
    val packageName: String,
    val appName: String,
    val targetLaneId: Long,
    val targetLaneName: String,
    val message: String,
    /** quickDismissed / total, 0..1; used for ordering. */
    val confidence: Float,
)

/**
 * Turns dismissal habits into re-screening prompts (R004), e.g.
 * "You dismissed 40 of 42 from Zalando in under 3 s. Move to Quiet?".
 *
 * Fires only with >= [MIN_SAMPLES] notifications and >= [QUICK_DISMISS_RATIO] dismissed
 * in under 3 s, and never when the app already sits in the target lane or a lane at least
 * as quiet (Quiet/Digest/Blocked policy).
 */
object ReScreenPromptEngine {
    const val MIN_SAMPLES = 20
    const val QUICK_DISMISS_RATIO = 0.90f
    const val QUICK_DISMISS_MS = 3_000L

    private val quietPolicies = setOf(LanePolicy.Quiet, LanePolicy.Digest, LanePolicy.Blocked)

    fun prompts(
        stats: List<AppDismissalStats>,
        lanes: List<Lane>,
        targetLaneId: Long = DefaultLanes.QUIET_ID,
    ): List<ReScreenPrompt> {
        val target = lanes.firstOrNull { it.id == targetLaneId } ?: return emptyList()
        val byId = lanes.associateBy { it.id }
        return stats.mapNotNull { s -> promptFor(s, target, byId) }
            .sortedByDescending { it.confidence }
    }

    private fun promptFor(s: AppDismissalStats, target: Lane, lanes: Map<Long, Lane>): ReScreenPrompt? {
        if (s.total < MIN_SAMPLES || s.quickDismissed <= 0) return null
        val quick = s.quickDismissed.coerceAtMost(s.total)
        val ratio = quick.toFloat() / s.total
        if (ratio < QUICK_DISMISS_RATIO) return null
        if (s.currentLaneId == target.id) return null
        if (lanes[s.currentLaneId]?.policy in quietPolicies) return null
        return ReScreenPrompt(
            packageName = s.packageName,
            appName = s.appName,
            targetLaneId = target.id,
            targetLaneName = target.name,
            message = "You dismissed $quick of ${s.total} from ${s.appName} in under ${QUICK_DISMISS_MS / 1000} s. Move to ${target.name}?",
            confidence = ratio,
        )
    }
}
