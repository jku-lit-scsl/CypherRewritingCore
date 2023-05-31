/*
 * Cypher Rewriting System
 * Copyright (C) 2023 Daniel Hofer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.jku.faw.symspace.cypherrewriter.core.cypher

import at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils.VariableState
import org.springframework.stereotype.Service

@Service
class RuleHelperImpl(private val filterTypeHelper: FilterTypeHelper, private val returnTypeHelper: ReturnTypeHelper) :
    RuleHelper {
    override fun ruleMatches(rule: Rule, variableStates: Map<String, VariableState>): Boolean {
        return rule.conditions.all { eval(it, variableStates, ConditionBoolean.AND) }
    }

    private fun eval(condition: Condition, variableStates: Map<String, VariableState>, outerOperation: ConditionBoolean): Boolean {
        return when (condition) {
            is ConditionCombination -> evalCombination(condition, variableStates, outerOperation)
            is ConditionExpression -> evalExpression(condition, variableStates)
            else -> throw NotImplementedError("An invalid condition instance was encountered.")
        }
    }

    private fun evalExpression(condition: ConditionExpression, variableStates: Map<String, VariableState>): Boolean {
        val state = variableStates[condition.variable]
            ?: throw IllegalArgumentException("The requested variable's state is unknown.")
        return statesMatch(state, condition.filterType, condition.returnType)
    }

    private fun evalCombination(condition: ConditionCombination, variableStates: Map<String, VariableState>, outerOperation: ConditionBoolean): Boolean {
        return when(condition.operation) {
            ConditionBoolean.AND -> condition.conditions.all { eval(it, variableStates, ConditionBoolean.AND) }
            ConditionBoolean.OR -> condition.conditions.any { eval(it, variableStates, ConditionBoolean.OR) }
            ConditionBoolean.NOT -> {
                when(outerOperation) {
                    ConditionBoolean.AND -> condition.conditions.all { !eval(it, variableStates, outerOperation) }
                    ConditionBoolean.OR -> condition.conditions.any { !eval(it, variableStates, outerOperation) }
                    ConditionBoolean.NOT -> throw IllegalArgumentException("${ConditionBoolean.NOT} is not valid as an outerOperation.")
                }
            }
        }
    }

    private fun statesMatch(
        queryVarState: VariableState, policyFilterType: FilterType, policyReturnType: ReturnType
    ): Boolean {
        val filterTypeResult = filterTypeHelper.filterTypesMatch(queryVarState.filterType, policyFilterType)
        val returnTypeResult = returnTypeHelper.returnTypesMatch(queryVarState.returnType, policyReturnType)

        return filterTypeResult && returnTypeResult
    }
}
