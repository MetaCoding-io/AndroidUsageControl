package com.usagecontrol.core.rules

import com.usagecontrol.core.model.ActionType
import com.usagecontrol.core.model.Rule
import com.usagecontrol.core.model.RuleAction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of executing a rule action.
 */
data class ActionResult(
    val action: RuleAction,
    val success: Boolean,
    val message: String? = null,
)

/**
 * Executes the actions defined by matched rules.
 * Delegates to pillar-specific handlers registered at runtime.
 */
@Singleton
class ActionExecutor @Inject constructor() {

    private val handlers = mutableMapOf<ActionType, ActionHandler>()

    fun registerHandler(actionType: ActionType, handler: ActionHandler) {
        handlers[actionType] = handler
    }

    /**
     * Execute all actions for a matched rule.
     * Returns results for each action attempted.
     */
    suspend fun execute(rule: Rule): List<ActionResult> {
        return rule.actions.map { action ->
            val handler = handlers[action.type]
            if (handler != null) {
                try {
                    handler.handle(action, rule)
                    ActionResult(action, success = true)
                } catch (e: Exception) {
                    ActionResult(action, success = false, message = e.message)
                }
            } else {
                ActionResult(action, success = false, message = "No handler for ${action.type}")
            }
        }
    }

    /**
     * Execute all actions for multiple matched rules (in priority order).
     */
    suspend fun executeAll(rules: List<Rule>): Map<Rule, List<ActionResult>> {
        return rules.associateWith { execute(it) }
    }
}

/**
 * Handler interface for a specific action type.
 * Implementations are registered per-pillar at app startup.
 */
fun interface ActionHandler {
    suspend fun handle(action: RuleAction, rule: Rule)
}
