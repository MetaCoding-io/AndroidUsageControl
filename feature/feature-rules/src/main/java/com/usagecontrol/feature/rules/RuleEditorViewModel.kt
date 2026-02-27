package com.usagecontrol.feature.rules

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagecontrol.core.database.dao.RuleDao
import com.usagecontrol.core.database.toEntity
import com.usagecontrol.core.database.toModel
import com.usagecontrol.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RuleEditorState(
    val name: String = "",
    val pillar: RulePillar = RulePillar.NOTIFICATION,
    val priority: Int = 0,
    val enabled: Boolean = true,
    val conditionGroups: List<ConditionGroup> = listOf(ConditionGroup(conditions = emptyList())),
    val actions: List<RuleAction> = emptyList(),
    val isEditing: Boolean = false,
    val editingRuleId: Long = 0,
    val isSaved: Boolean = false,
)

@HiltViewModel
class RuleEditorViewModel @Inject constructor(
    private val ruleDao: RuleDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val ruleId: Long = savedStateHandle.get<Long>("ruleId") ?: 0L
    private val pillarArg: String? = savedStateHandle.get<String>("pillar")

    private val _state = MutableStateFlow(RuleEditorState())
    val state: StateFlow<RuleEditorState> = _state.asStateFlow()

    init {
        if (ruleId > 0) {
            loadRule(ruleId)
        } else if (pillarArg != null) {
            _state.update { it.copy(pillar = RulePillar.valueOf(pillarArg)) }
        }
    }

    private fun loadRule(id: Long) {
        viewModelScope.launch {
            val entity = ruleDao.getById(id) ?: return@launch
            val rule = entity.toModel()
            _state.update {
                it.copy(
                    name = rule.name,
                    pillar = rule.pillar,
                    priority = rule.priority,
                    enabled = rule.enabled,
                    conditionGroups = rule.conditionGroups.ifEmpty {
                        // Migrate flat conditions into a single group
                        listOf(ConditionGroup(conditions = rule.conditions))
                    },
                    actions = rule.actions,
                    isEditing = true,
                    editingRuleId = rule.id,
                )
            }
        }
    }

    fun updateName(name: String) {
        _state.update { it.copy(name = name) }
    }

    fun updatePillar(pillar: RulePillar) {
        _state.update { it.copy(pillar = pillar, actions = emptyList()) }
    }

    fun updatePriority(priority: Int) {
        _state.update { it.copy(priority = priority) }
    }

    fun addConditionGroup() {
        _state.update {
            it.copy(conditionGroups = it.conditionGroups + ConditionGroup(conditions = emptyList()))
        }
    }

    fun removeConditionGroup(groupIndex: Int) {
        _state.update {
            val groups = it.conditionGroups.toMutableList()
            if (groups.size > 1) {
                groups.removeAt(groupIndex)
            }
            it.copy(conditionGroups = groups)
        }
    }

    fun addCondition(groupIndex: Int, condition: RuleCondition) {
        _state.update {
            val groups = it.conditionGroups.toMutableList()
            if (groupIndex < groups.size) {
                val group = groups[groupIndex]
                groups[groupIndex] = group.copy(conditions = group.conditions + condition)
            }
            it.copy(conditionGroups = groups)
        }
    }

    fun removeCondition(groupIndex: Int, conditionIndex: Int) {
        _state.update {
            val groups = it.conditionGroups.toMutableList()
            if (groupIndex < groups.size) {
                val group = groups[groupIndex]
                val conditions = group.conditions.toMutableList()
                if (conditionIndex < conditions.size) {
                    conditions.removeAt(conditionIndex)
                    groups[groupIndex] = group.copy(conditions = conditions)
                }
            }
            it.copy(conditionGroups = groups)
        }
    }

    fun addAction(action: RuleAction) {
        _state.update { it.copy(actions = it.actions + action) }
    }

    fun removeAction(index: Int) {
        _state.update {
            val actions = it.actions.toMutableList()
            if (index < actions.size) {
                actions.removeAt(index)
            }
            it.copy(actions = actions)
        }
    }

    fun save() {
        val current = _state.value
        if (current.name.isBlank()) return
        if (current.actions.isEmpty()) return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val rule = Rule(
                id = if (current.isEditing) current.editingRuleId else 0,
                name = current.name,
                pillar = current.pillar,
                priority = current.priority,
                enabled = current.enabled,
                conditionGroups = current.conditionGroups.filter { it.conditions.isNotEmpty() },
                conditions = emptyList(), // Using groups instead
                actions = current.actions,
                createdAt = if (current.isEditing) 0 else now, // preserves original on update
                updatedAt = now,
            )

            if (current.isEditing) {
                val existing = ruleDao.getById(current.editingRuleId)
                if (existing != null) {
                    ruleDao.update(rule.copy(createdAt = existing.createdAt).toEntity())
                }
            } else {
                ruleDao.insert(rule.toEntity())
            }
            _state.update { it.copy(isSaved = true) }
        }
    }
}
