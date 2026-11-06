package com.usagecontrol.core.rules

import com.usagecontrol.core.model.DefaultLanes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReScreenPromptEngineTest {

    private val lanes = DefaultLanes.all

    private fun stats(total: Int, quick: Int, current: Long? = DefaultLanes.BATCHED_ID, name: String = "Zalando") =
        AppDismissalStats("com.$name".lowercase(), name, total, quick, current)

    @Test fun habitualQuickDismissProducesPrompt() {
        val p = ReScreenPromptEngine.prompts(listOf(stats(42, 40)), lanes).single()
        assertEquals("You dismissed 40 of 42 from Zalando in under 3 s. Move to Quiet?", p.message)
        assertEquals(DefaultLanes.QUIET_ID, p.targetLaneId)
        assertEquals("Quiet", p.targetLaneName)
    }

    @Test fun exactThresholdsPass() {
        assertEquals(1, ReScreenPromptEngine.prompts(listOf(stats(20, 18)), lanes).size)
    }

    @Test fun insufficientSamplesNoPrompt() {
        assertTrue(ReScreenPromptEngine.prompts(listOf(stats(19, 19)), lanes).isEmpty())
    }

    @Test fun belowRatioNoPrompt() {
        assertTrue(ReScreenPromptEngine.prompts(listOf(stats(100, 89)), lanes).isEmpty())
    }

    @Test fun alreadyInTargetLaneNoPrompt() {
        assertTrue(ReScreenPromptEngine.prompts(listOf(stats(42, 40, DefaultLanes.QUIET_ID)), lanes).isEmpty())
    }

    @Test fun alreadyInQuieterLaneNoPrompt() {
        assertTrue(ReScreenPromptEngine.prompts(listOf(stats(42, 40, DefaultLanes.BLOCKED_ID)), lanes).isEmpty())
        assertTrue(ReScreenPromptEngine.prompts(listOf(stats(42, 40, DefaultLanes.DIGEST_ID)), lanes).isEmpty())
    }

    @Test fun customTargetLaneUsesItsName() {
        val p = ReScreenPromptEngine.prompts(listOf(stats(30, 30)), lanes, DefaultLanes.BLOCKED_ID).single()
        assertTrue(p.message.endsWith("Move to Blocked?"))
    }

    @Test fun missingTargetLaneNoPrompt() {
        assertTrue(ReScreenPromptEngine.prompts(listOf(stats(42, 40)), lanes, targetLaneId = 99L).isEmpty())
    }

    @Test fun malformedCountsAreClampedOrRejected() {
        assertTrue(ReScreenPromptEngine.prompts(listOf(stats(0, 0)), lanes).isEmpty())
        assertTrue(ReScreenPromptEngine.prompts(listOf(stats(25, -3)), lanes).isEmpty())
        val clamped = ReScreenPromptEngine.prompts(listOf(stats(25, 30)), lanes).single()
        assertEquals("You dismissed 25 of 25 from Zalando in under 3 s. Move to Quiet?", clamped.message)
    }

    @Test fun unknownCurrentLaneStillPrompts() {
        assertEquals(1, ReScreenPromptEngine.prompts(listOf(stats(42, 40, current = null)), lanes).size)
    }

    @Test fun sortedByConfidence() {
        val out = ReScreenPromptEngine.prompts(listOf(stats(100, 91, name = "A"), stats(20, 20, name = "B")), lanes)
        assertEquals(listOf("B", "A"), out.map { it.appName })
    }
}
