package com.usagecontrol.feature.rules

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usagecontrol.core.database.dao.RuleDao
import com.usagecontrol.core.database.toEntity
import com.usagecontrol.core.database.toModel
import com.usagecontrol.core.model.Rule
import com.usagecontrol.core.model.RulePillar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RuleListViewModel @Inject constructor(
    private val ruleDao: RuleDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val pillarArg: String? = savedStateHandle.get<String>("pillar")

    val rules: StateFlow<List<Rule>> = if (pillarArg != null) {
        ruleDao.getByPillarFlow(pillarArg)
    } else {
        ruleDao.getAllFlow()
    }.map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pillar: RulePillar? = pillarArg?.let { RulePillar.valueOf(it) }

    fun toggleRule(ruleId: Long, enabled: Boolean) {
        viewModelScope.launch {
            ruleDao.setEnabled(ruleId, enabled)
        }
    }

    fun deleteRule(rule: Rule) {
        viewModelScope.launch {
            ruleDao.delete(rule.toEntity())
        }
    }
}
