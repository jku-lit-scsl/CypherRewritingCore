package at.jku.faw.symspace.cypherrewriter.core.cypher

interface FilterTypeHelper {
    fun mergeFilterTypes(existingFilterType: FilterType, newFilterType: FilterType): FilterType
    fun filterTypesMatch(queryFilterType: FilterType, policyFilterType: FilterType): Boolean
}
