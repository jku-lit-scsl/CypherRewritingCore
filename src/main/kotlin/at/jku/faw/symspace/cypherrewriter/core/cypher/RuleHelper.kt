package at.jku.faw.symspace.cypherrewriter.core.cypher

import at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils.VariableState

interface RuleHelper {
    fun ruleMatches(rule: Rule, variableStates: Map<String, VariableState>): Boolean
}
