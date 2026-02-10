package at.jku.faw.symspace.cypherrewriter.core.cypher

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/*
 * This test class was generated using AI tools (Claude Code)
 */
class ReturnTypeHelperTest {

    private val helper: ReturnTypeHelper = ReturnTypeHelperImpl()

    // mergeReturnTypes tests

    @Test
    fun mergeReturnTypesAnyAsNewReturnTypeThrowsException() {
        assertThrows(IllegalArgumentException::class.java) {
            helper.mergeReturnTypes(ReturnType.NO_RETURN, ReturnType.ANY)
        }
    }

    @Test
    fun mergeReturnTypesHigherOrdinalWins() {
        val result = helper.mergeReturnTypes(ReturnType.NO_RETURN, ReturnType.RETURNED_AS_VALUE)
        assertEquals(ReturnType.RETURNED_AS_VALUE, result)
    }

    @Test
    fun mergeReturnTypesExistingHigherStays() {
        val result = helper.mergeReturnTypes(ReturnType.RETURNED_AS_VALUE, ReturnType.NO_RETURN)
        assertEquals(ReturnType.RETURNED_AS_VALUE, result)
    }

    @Test
    fun mergeReturnTypesSameValuesReturnsSame() {
        val result = helper.mergeReturnTypes(ReturnType.AGGREGATED, ReturnType.AGGREGATED)
        assertEquals(ReturnType.AGGREGATED, result)
    }

    @Test
    fun mergeReturnTypesAggregatedAndReturnedAsValue() {
        val result = helper.mergeReturnTypes(ReturnType.AGGREGATED, ReturnType.RETURNED_AS_VALUE)
        assertEquals(ReturnType.RETURNED_AS_VALUE, result)
    }

    // returnTypesMatch tests

    @Test
    fun returnTypesMatchPolicyAnyMatchesAnything() {
        assertTrue(helper.returnTypesMatch(ReturnType.NO_RETURN, ReturnType.ANY))
        assertTrue(helper.returnTypesMatch(ReturnType.RETURNED_AS_VALUE, ReturnType.ANY))
        assertTrue(helper.returnTypesMatch(ReturnType.AGGREGATED, ReturnType.ANY))
    }

    @Test
    fun returnTypesMatchQueryAnyMatchesAnything() {
        assertTrue(helper.returnTypesMatch(ReturnType.ANY, ReturnType.NO_RETURN))
        assertTrue(helper.returnTypesMatch(ReturnType.ANY, ReturnType.RETURNED_AS_VALUE))
        assertTrue(helper.returnTypesMatch(ReturnType.ANY, ReturnType.AGGREGATED))
    }

    @Test
    fun returnTypesMatchSameTypesMatch() {
        assertTrue(helper.returnTypesMatch(ReturnType.NO_RETURN, ReturnType.NO_RETURN))
        assertTrue(helper.returnTypesMatch(ReturnType.RETURNED_AS_VALUE, ReturnType.RETURNED_AS_VALUE))
        assertTrue(helper.returnTypesMatch(ReturnType.AGGREGATED, ReturnType.AGGREGATED))
    }

    @Test
    fun returnTypesMatchAnyReturnMatchesReturnedAsValue() {
        assertTrue(helper.returnTypesMatch(ReturnType.RETURNED_AS_VALUE, ReturnType.ANY_RETURN))
    }

    @Test
    fun returnTypesMatchAnyReturnMatchesAggregated() {
        assertTrue(helper.returnTypesMatch(ReturnType.AGGREGATED, ReturnType.ANY_RETURN))
    }

    @Test
    fun returnTypesMatchAnyReturnDoesNotMatchNoReturn() {
        assertFalse(helper.returnTypesMatch(ReturnType.NO_RETURN, ReturnType.ANY_RETURN))
    }

    @Test
    fun returnTypesMatchDifferentNonAnyTypesDontMatch() {
        assertFalse(helper.returnTypesMatch(ReturnType.NO_RETURN, ReturnType.RETURNED_AS_VALUE))
        assertFalse(helper.returnTypesMatch(ReturnType.RETURNED_AS_VALUE, ReturnType.NO_RETURN))
        assertFalse(helper.returnTypesMatch(ReturnType.NO_RETURN, ReturnType.AGGREGATED))
        assertFalse(helper.returnTypesMatch(ReturnType.AGGREGATED, ReturnType.NO_RETURN))
    }
}
