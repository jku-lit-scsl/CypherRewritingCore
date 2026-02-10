package at.jku.faw.symspace.cypherrewriter.core.cypher

import at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils.VariableState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/*
 * This test class was generated using AI tools (Claude Code)
 */
class RuleHelperTest {

    private val filterTypeHelper: FilterTypeHelper = FilterTypeHelperImpl()
    private val returnTypeHelper: ReturnTypeHelper = ReturnTypeHelperImpl()
    private val ruleHelper: RuleHelper = RuleHelperImpl(filterTypeHelper, returnTypeHelper)

    private fun createRule(vararg conditions: Condition): Rule {
        return Rule("testRule", "u", conditions.toList(), "filter1", AuthorizationLevel.OWNER_LEVEL)
    }

    // Empty conditions -> true

    @Test
    fun emptyConditionsReturnsTrue() {
        val rule = createRule()
        val states = mapOf("u" to VariableState(ReturnType.NO_RETURN, FilterType.NO_FILTER))
        assertTrue(ruleHelper.ruleMatches(rule, states))
    }

    // Single ConditionExpression

    @Test
    fun singleConditionExpressionMatches() {
        val condition = ConditionExpression("u", FilterType.FILTERED, ReturnType.RETURNED_AS_VALUE)
        val rule = createRule(condition)
        val states = mapOf("u" to VariableState(ReturnType.RETURNED_AS_VALUE, FilterType.FILTERED))
        assertTrue(ruleHelper.ruleMatches(rule, states))
    }

    @Test
    fun singleConditionExpressionDoesNotMatch() {
        val condition = ConditionExpression("u", FilterType.FILTERED, ReturnType.RETURNED_AS_VALUE)
        val rule = createRule(condition)
        val states = mapOf("u" to VariableState(ReturnType.NO_RETURN, FilterType.NO_FILTER))
        assertFalse(ruleHelper.ruleMatches(rule, states))
    }

    @Test
    fun singleConditionExpressionFilterMatchReturnNoMatch() {
        val condition = ConditionExpression("u", FilterType.FILTERED, ReturnType.RETURNED_AS_VALUE)
        val rule = createRule(condition)
        val states = mapOf("u" to VariableState(ReturnType.NO_RETURN, FilterType.FILTERED))
        assertFalse(ruleHelper.ruleMatches(rule, states))
    }

    // ConditionCombination AND

    @Test
    fun conditionAndAllMatch() {
        val cond1 = ConditionExpression("u", FilterType.ANY, ReturnType.RETURNED_AS_VALUE)
        val cond2 = ConditionExpression("h", FilterType.ANY, ReturnType.RETURNED_AS_VALUE)
        val andCombination = ConditionCombination(ConditionBoolean.AND, cond1, cond2)
        val rule = createRule(andCombination)
        val states = mapOf(
            "u" to VariableState(ReturnType.RETURNED_AS_VALUE, FilterType.NO_FILTER),
            "h" to VariableState(ReturnType.RETURNED_AS_VALUE, FilterType.NO_FILTER)
        )
        assertTrue(ruleHelper.ruleMatches(rule, states))
    }

    @Test
    fun conditionAndOneDoesNotMatch() {
        val cond1 = ConditionExpression("u", FilterType.ANY, ReturnType.RETURNED_AS_VALUE)
        val cond2 = ConditionExpression("h", FilterType.ANY, ReturnType.RETURNED_AS_VALUE)
        val andCombination = ConditionCombination(ConditionBoolean.AND, cond1, cond2)
        val rule = createRule(andCombination)
        val states = mapOf(
            "u" to VariableState(ReturnType.RETURNED_AS_VALUE, FilterType.NO_FILTER),
            "h" to VariableState(ReturnType.NO_RETURN, FilterType.NO_FILTER)
        )
        assertFalse(ruleHelper.ruleMatches(rule, states))
    }

    // ConditionCombination OR

    @Test
    fun conditionOrOneMatches() {
        val cond1 = ConditionExpression("u", FilterType.ANY, ReturnType.RETURNED_AS_VALUE)
        val cond2 = ConditionExpression("h", FilterType.ANY, ReturnType.RETURNED_AS_VALUE)
        val orCombination = ConditionCombination(ConditionBoolean.OR, cond1, cond2)
        val rule = createRule(orCombination)
        val states = mapOf(
            "u" to VariableState(ReturnType.RETURNED_AS_VALUE, FilterType.NO_FILTER),
            "h" to VariableState(ReturnType.NO_RETURN, FilterType.NO_FILTER)
        )
        assertTrue(ruleHelper.ruleMatches(rule, states))
    }

    @Test
    fun conditionOrNoneMatches() {
        val cond1 = ConditionExpression("u", FilterType.ANY, ReturnType.RETURNED_AS_VALUE)
        val cond2 = ConditionExpression("h", FilterType.ANY, ReturnType.RETURNED_AS_VALUE)
        val orCombination = ConditionCombination(ConditionBoolean.OR, cond1, cond2)
        val rule = createRule(orCombination)
        val states = mapOf(
            "u" to VariableState(ReturnType.NO_RETURN, FilterType.NO_FILTER),
            "h" to VariableState(ReturnType.NO_RETURN, FilterType.NO_FILTER)
        )
        assertFalse(ruleHelper.ruleMatches(rule, states))
    }

    // ConditionCombination NOT

    @Test
    fun conditionNotUnderAndNegatesAllConditions() {
        val cond = ConditionExpression("u", FilterType.ANY, ReturnType.RETURNED_AS_VALUE)
        val notCombination = ConditionCombination(ConditionBoolean.NOT, cond)
        val rule = createRule(notCombination)

        val statesNotReturned = mapOf("u" to VariableState(ReturnType.NO_RETURN, FilterType.NO_FILTER))
        assertTrue(ruleHelper.ruleMatches(rule, statesNotReturned))

        val statesReturned = mapOf("u" to VariableState(ReturnType.RETURNED_AS_VALUE, FilterType.NO_FILTER))
        assertFalse(ruleHelper.ruleMatches(rule, statesReturned))
    }

    @Test
    fun conditionNotUnderOrNegates() {
        val cond1 = ConditionExpression("u", FilterType.ANY, ReturnType.RETURNED_AS_VALUE)
        val cond2 = ConditionExpression("h", FilterType.ANY, ReturnType.RETURNED_AS_VALUE)
        val notCombination = ConditionCombination(ConditionBoolean.NOT, cond1, cond2)
        val orOuter = ConditionCombination(ConditionBoolean.OR, notCombination)
        val rule = createRule(orOuter)

        // NOT under OR: any { !eval(it) } - at least one must be negated to true
        val statesNeitherReturned = mapOf(
            "u" to VariableState(ReturnType.NO_RETURN, FilterType.NO_FILTER),
            "h" to VariableState(ReturnType.NO_RETURN, FilterType.NO_FILTER)
        )
        assertTrue(ruleHelper.ruleMatches(rule, statesNeitherReturned))

        val statesBothReturned = mapOf(
            "u" to VariableState(ReturnType.RETURNED_AS_VALUE, FilterType.NO_FILTER),
            "h" to VariableState(ReturnType.RETURNED_AS_VALUE, FilterType.NO_FILTER)
        )
        assertFalse(ruleHelper.ruleMatches(rule, statesBothReturned))
    }

    // Nested combination OR(AND(...), AND(...))

    @Test
    fun nestedOrOfAndCombinations() {
        val condU = ConditionExpression("u", FilterType.FILTERED, ReturnType.ANY)
        val condH = ConditionExpression("h", FilterType.ANY, ReturnType.RETURNED_AS_VALUE)
        val and1 = ConditionCombination(ConditionBoolean.AND, condU, condH)

        val condU2 = ConditionExpression("u", FilterType.ANY, ReturnType.RETURNED_AS_VALUE)
        val condH2 = ConditionExpression("h", FilterType.FILTERED, ReturnType.ANY)
        val and2 = ConditionCombination(ConditionBoolean.AND, condU2, condH2)

        val orCombination = ConditionCombination(ConditionBoolean.OR, and1, and2)
        val rule = createRule(orCombination)

        // First AND branch matches: u is FILTERED, h is RETURNED_AS_VALUE
        val states1 = mapOf(
            "u" to VariableState(ReturnType.NO_RETURN, FilterType.FILTERED),
            "h" to VariableState(ReturnType.RETURNED_AS_VALUE, FilterType.NO_FILTER)
        )
        assertTrue(ruleHelper.ruleMatches(rule, states1))

        // Second AND branch matches: u is RETURNED_AS_VALUE, h is FILTERED
        val states2 = mapOf(
            "u" to VariableState(ReturnType.RETURNED_AS_VALUE, FilterType.NO_FILTER),
            "h" to VariableState(ReturnType.NO_RETURN, FilterType.FILTERED)
        )
        assertTrue(ruleHelper.ruleMatches(rule, states2))

        // Neither branch matches
        val states3 = mapOf(
            "u" to VariableState(ReturnType.NO_RETURN, FilterType.NO_FILTER),
            "h" to VariableState(ReturnType.NO_RETURN, FilterType.NO_FILTER)
        )
        assertFalse(ruleHelper.ruleMatches(rule, states3))
    }

    // Unknown variable -> Exception

    @Test
    fun unknownVariableThrowsException() {
        val condition = ConditionExpression("unknown", FilterType.ANY, ReturnType.ANY)
        val rule = createRule(condition)
        val states = mapOf("u" to VariableState(ReturnType.NO_RETURN, FilterType.NO_FILTER))
        assertThrows(IllegalArgumentException::class.java) {
            ruleHelper.ruleMatches(rule, states)
        }
    }

    // Multiple top-level conditions (implicit AND)

    @Test
    fun multipleTopLevelConditionsImplicitAndAllMatch() {
        val cond1 = ConditionExpression("u", FilterType.ANY, ReturnType.RETURNED_AS_VALUE)
        val cond2 = ConditionExpression("h", FilterType.FILTERED, ReturnType.ANY)
        val rule = createRule(cond1, cond2)
        val states = mapOf(
            "u" to VariableState(ReturnType.RETURNED_AS_VALUE, FilterType.NO_FILTER),
            "h" to VariableState(ReturnType.NO_RETURN, FilterType.FILTERED)
        )
        assertTrue(ruleHelper.ruleMatches(rule, states))
    }

    @Test
    fun multipleTopLevelConditionsImplicitAndOneDoesNotMatch() {
        val cond1 = ConditionExpression("u", FilterType.ANY, ReturnType.RETURNED_AS_VALUE)
        val cond2 = ConditionExpression("h", FilterType.FILTERED, ReturnType.ANY)
        val rule = createRule(cond1, cond2)
        val states = mapOf(
            "u" to VariableState(ReturnType.RETURNED_AS_VALUE, FilterType.NO_FILTER),
            "h" to VariableState(ReturnType.NO_RETURN, FilterType.NO_FILTER)
        )
        assertFalse(ruleHelper.ruleMatches(rule, states))
    }
}
