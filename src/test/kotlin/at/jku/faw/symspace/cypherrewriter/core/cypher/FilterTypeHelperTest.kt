package at.jku.faw.symspace.cypherrewriter.core.cypher

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/*
 * This test class was generated using AI tools (Claude Code)
 */
class FilterTypeHelperTest {

    private val helper: FilterTypeHelper = FilterTypeHelperImpl()

    // mergeFilterTypes tests

    @Test
    fun mergeFilterTypesAnyAsNewFilterTypeThrowsException() {
        assertThrows(IllegalArgumentException::class.java) {
            helper.mergeFilterTypes(FilterType.NO_FILTER, FilterType.ANY)
        }
    }

    @Test
    fun mergeFilterTypesHigherOrdinalWins() {
        val result = helper.mergeFilterTypes(FilterType.NO_FILTER, FilterType.FILTERED)
        assertEquals(FilterType.FILTERED, result)
    }

    @Test
    fun mergeFilterTypesExistingHigherStays() {
        val result = helper.mergeFilterTypes(FilterType.FILTERED, FilterType.NO_FILTER)
        assertEquals(FilterType.FILTERED, result)
    }

    @Test
    fun mergeFilterTypesSameValuesReturnsSame() {
        val result = helper.mergeFilterTypes(FilterType.FILTERED, FilterType.FILTERED)
        assertEquals(FilterType.FILTERED, result)
    }

    @Test
    fun mergeFilterTypesSameNoFilterReturnsSame() {
        val result = helper.mergeFilterTypes(FilterType.NO_FILTER, FilterType.NO_FILTER)
        assertEquals(FilterType.NO_FILTER, result)
    }

    // filterTypesMatch tests

    @Test
    fun filterTypesMatchPolicyAnyMatchesAnything() {
        assertTrue(helper.filterTypesMatch(FilterType.NO_FILTER, FilterType.ANY))
        assertTrue(helper.filterTypesMatch(FilterType.FILTERED, FilterType.ANY))
    }

    @Test
    fun filterTypesMatchQueryAnyMatchesAnything() {
        assertTrue(helper.filterTypesMatch(FilterType.ANY, FilterType.NO_FILTER))
        assertTrue(helper.filterTypesMatch(FilterType.ANY, FilterType.FILTERED))
    }

    @Test
    fun filterTypesMatchSameTypesMatch() {
        assertTrue(helper.filterTypesMatch(FilterType.NO_FILTER, FilterType.NO_FILTER))
        assertTrue(helper.filterTypesMatch(FilterType.FILTERED, FilterType.FILTERED))
        assertTrue(helper.filterTypesMatch(FilterType.ANY, FilterType.ANY))
    }

    @Test
    fun filterTypesMatchDifferentNonAnyTypesDontMatch() {
        assertFalse(helper.filterTypesMatch(FilterType.NO_FILTER, FilterType.FILTERED))
        assertFalse(helper.filterTypesMatch(FilterType.FILTERED, FilterType.NO_FILTER))
    }
}
