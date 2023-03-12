package at.jku.faw.symspace.cypherrewriter.core.cypher

import org.springframework.stereotype.Service

@Service
class FilterTypeHelperImpl : FilterTypeHelper {
    override fun mergeFilterTypes(existingFilterType: FilterType, newFilterType: FilterType): FilterType {
        if (newFilterType == FilterType.ANY) {
            throw IllegalArgumentException("$newFilterType must not be used here.")
        }

        return if (existingFilterType < newFilterType) {
            newFilterType
        } else {
            existingFilterType
        }
    }

    override fun filterTypesMatch(queryFilterType: FilterType, policyFilterType: FilterType): Boolean {
        return policyFilterType == FilterType.ANY
                || queryFilterType == FilterType.ANY
                || queryFilterType == policyFilterType
    }
}
