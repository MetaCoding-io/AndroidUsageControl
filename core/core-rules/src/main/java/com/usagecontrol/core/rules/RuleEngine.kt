package com.usagecontrol.core.rules

import com.usagecontrol.core.database.dao.RuleDao
import com.usagecontrol.core.database.toModel
import com.usagecontrol.core.model.Rule
import com.usagecontrol.core.model.RulePillar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleEngine @Inject constructor(
    private val ruleDao: RuleDao,
    private val conditionEvaluator: ConditionEvaluator,
) {

    /**
     * Find all matching rules for a given pillar and evaluation context.
     * Returns rules sorted by priority (highest first).
     */
    suspend fun findMatchingRules(
        pillar: RulePillar,
        context: EvaluationContext,
    ): List<Rule> {
        val rules = ruleDao.getEnabledByPillar(pillar.name).map { it.toModel() }
        return rules.filter { rule ->
            conditionEvaluator.evaluateAll(rule.conditions, context)
        }
    }

    /**
     * Check if any enabled rule for the given pillar matches the context.
     */
    suspend fun hasMatchingRule(
        pillar: RulePillar,
        context: EvaluationContext,
    ): Boolean {
        return findMatchingRules(pillar, context).isNotEmpty()
    }
}
