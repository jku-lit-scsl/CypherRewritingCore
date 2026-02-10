package at.jku.faw.symspace.cypherrewriter.core.cypher

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/*
 * This test class was generated using AI tools (Claude Code)
 */
class LabelMatcherTest {

    private val matcher: LabelMatcher = LabelMatcherImpl()

    // anyAsteriskLabels tests

    @Test
    fun anyAsteriskLabelsAsteriskInQuery() {
        assertTrue(matcher.anyAsteriskLabels(setOf("*"), setOf("User")))
    }

    @Test
    fun anyAsteriskLabelsAsteriskInPolicy() {
        assertTrue(matcher.anyAsteriskLabels(setOf("User"), setOf("*")))
    }

    @Test
    fun anyAsteriskLabelsAsteriskInBoth() {
        assertTrue(matcher.anyAsteriskLabels(setOf("*"), setOf("*")))
    }

    @Test
    fun anyAsteriskLabelsNoAsterisk() {
        assertFalse(matcher.anyAsteriskLabels(setOf("User"), setOf("Host")))
    }

    @Test
    fun anyAsteriskLabelsEmptySets() {
        assertFalse(matcher.anyAsteriskLabels(emptySet(), emptySet()))
    }

    // matchLabelsExactly tests

    @Test
    fun matchLabelsExactlySameLabels() {
        assertTrue(matcher.matchLabelsExactly(setOf("User"), setOf("User")))
    }

    @Test
    fun matchLabelsExactlyDifferentLabels() {
        assertFalse(matcher.matchLabelsExactly(setOf("User"), setOf("Host")))
    }

    @Test
    fun matchLabelsExactlySubsetDoesNotMatch() {
        assertFalse(matcher.matchLabelsExactly(setOf("User", "Admin"), setOf("User")))
    }

    @Test
    fun matchLabelsExactlyAsteriskMatches() {
        assertTrue(matcher.matchLabelsExactly(setOf("*"), setOf("User")))
        assertTrue(matcher.matchLabelsExactly(setOf("User"), setOf("*")))
    }

    @Test
    fun matchLabelsExactlyEmptyQueryWithMatchEmptyTrue() {
        assertTrue(matcher.matchLabelsExactly(emptySet(), setOf("User"), matchEmptyQueryLabels = true))
    }

    @Test
    fun matchLabelsExactlyEmptyQueryWithMatchEmptyFalse() {
        assertFalse(matcher.matchLabelsExactly(emptySet(), setOf("User"), matchEmptyQueryLabels = false))
    }

    @Test
    fun matchLabelsExactlyMultipleSameLabels() {
        assertTrue(matcher.matchLabelsExactly(setOf("User", "Admin"), setOf("User", "Admin")))
    }

    // matchLabelsQueryContainsAllPolicy tests

    @Test
    fun matchLabelsQueryContainsAllPolicySupersetMatches() {
        assertTrue(matcher.matchLabelsQueryContainsAllPolicy(setOf("User", "Admin"), setOf("User")))
    }

    @Test
    fun matchLabelsQueryContainsAllPolicyExactMatches() {
        assertTrue(matcher.matchLabelsQueryContainsAllPolicy(setOf("User"), setOf("User")))
    }

    @Test
    fun matchLabelsQueryContainsAllPolicySubsetDoesNotMatch() {
        assertFalse(matcher.matchLabelsQueryContainsAllPolicy(setOf("User"), setOf("User", "Admin")))
    }

    @Test
    fun matchLabelsQueryContainsAllPolicyAsteriskMatches() {
        assertTrue(matcher.matchLabelsQueryContainsAllPolicy(setOf("*"), setOf("User")))
        assertTrue(matcher.matchLabelsQueryContainsAllPolicy(setOf("User"), setOf("*")))
    }

    @Test
    fun matchLabelsQueryContainsAllPolicyEmptyQueryWithMatchEmpty() {
        assertTrue(matcher.matchLabelsQueryContainsAllPolicy(emptySet(), setOf("User"), matchEmptyQueryLabels = true))
    }

    @Test
    fun matchLabelsQueryContainsAllPolicyEmptyQueryWithoutMatchEmpty() {
        assertFalse(matcher.matchLabelsQueryContainsAllPolicy(emptySet(), setOf("User"), matchEmptyQueryLabels = false))
    }

    // matchLabelsQueryContainsAnyPolicy tests

    @Test
    fun matchLabelsQueryContainsAnyPolicyOverlapPresent() {
        assertTrue(matcher.matchLabelsQueryContainsAnyPolicy(setOf("User", "Admin"), setOf("Admin", "Host")))
    }

    @Test
    fun matchLabelsQueryContainsAnyPolicyNoOverlap() {
        assertFalse(matcher.matchLabelsQueryContainsAnyPolicy(setOf("User"), setOf("Host")))
    }

    @Test
    fun matchLabelsQueryContainsAnyPolicyAsteriskMatches() {
        assertTrue(matcher.matchLabelsQueryContainsAnyPolicy(setOf("*"), setOf("User")))
        assertTrue(matcher.matchLabelsQueryContainsAnyPolicy(setOf("User"), setOf("*")))
    }

    @Test
    fun matchLabelsQueryContainsAnyPolicyEmptyQueryWithMatchEmpty() {
        assertTrue(matcher.matchLabelsQueryContainsAnyPolicy(emptySet(), setOf("User"), matchEmptyQueryLabels = true))
    }

    @Test
    fun matchLabelsQueryContainsAnyPolicyEmptyQueryWithoutMatchEmpty() {
        assertFalse(matcher.matchLabelsQueryContainsAnyPolicy(emptySet(), setOf("User"), matchEmptyQueryLabels = false))
    }

    @Test
    fun matchLabelsQueryContainsAnyPolicyEmptyPolicy() {
        assertFalse(matcher.matchLabelsQueryContainsAnyPolicy(setOf("User"), emptySet()))
    }

    // buildLabelMatchMap tests

    @Test
    fun buildLabelMatchMapExactStrategy() {
        val strategy = mapOf("u" to PatternMatchStrategy(LabelMatchStrategy.EXACT, false))
        val matchMap = matcher.buildLabelMatchMap(strategy)

        assertTrue(matchMap.containsKey("u"))
        assertTrue(matchMap["u"]!!(setOf("User"), setOf("User")))
        assertFalse(matchMap["u"]!!(setOf("User", "Admin"), setOf("User")))
    }

    @Test
    fun buildLabelMatchMapContainsAllStrategy() {
        val strategy = mapOf("u" to PatternMatchStrategy(LabelMatchStrategy.CONTAINS_ALL, false))
        val matchMap = matcher.buildLabelMatchMap(strategy)

        assertTrue(matchMap.containsKey("u"))
        assertTrue(matchMap["u"]!!(setOf("User", "Admin"), setOf("User")))
        assertFalse(matchMap["u"]!!(setOf("User"), setOf("User", "Admin")))
    }

    @Test
    fun buildLabelMatchMapContainsAnyStrategy() {
        val strategy = mapOf("u" to PatternMatchStrategy(LabelMatchStrategy.CONTAINS_ANY, false))
        val matchMap = matcher.buildLabelMatchMap(strategy)

        assertTrue(matchMap.containsKey("u"))
        assertTrue(matchMap["u"]!!(setOf("User", "Admin"), setOf("Admin", "Host")))
        assertFalse(matchMap["u"]!!(setOf("User"), setOf("Host")))
    }

    @Test
    fun buildLabelMatchMapMatchEmptyLabelsPassedThrough() {
        val strategyWithEmpty = mapOf("u" to PatternMatchStrategy(LabelMatchStrategy.EXACT, true))
        val matchMapWithEmpty = matcher.buildLabelMatchMap(strategyWithEmpty)
        assertTrue(matchMapWithEmpty["u"]!!(emptySet(), setOf("User")))

        val strategyWithoutEmpty = mapOf("u" to PatternMatchStrategy(LabelMatchStrategy.EXACT, false))
        val matchMapWithoutEmpty = matcher.buildLabelMatchMap(strategyWithoutEmpty)
        assertFalse(matchMapWithoutEmpty["u"]!!(emptySet(), setOf("User")))
    }
}
