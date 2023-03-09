package at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils

import at.jku.faw.symspace.cypherrewriter.core.cypher.FilterType
import at.jku.faw.symspace.cypherrewriter.core.cypher.ReturnType

data class VariableState(var returnType: ReturnType = ReturnType.NO_RETURN, var filterType: FilterType = FilterType.NO_FILTER)
