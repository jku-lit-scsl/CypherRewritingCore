package at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils

data class Context(
    val policy: PolicyContext,
    val query: QueryContext = QueryContext(),
    val structuralMatches: MutableSet<StructuralMatch> = mutableSetOf(),
    val possibleVarMappings: MutableMap<Variable, MutableSet<Variable>> = mutableMapOf(),
    val validVarMappings: MutableSet<Mapping> = mutableSetOf(),
    val filteredVariables: MutableList<Variable> = mutableListOf(),
    val returnedVariables: MutableList<Variable> = mutableListOf(),
    var inReturnSubTree: Boolean = false,
)
