package at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils

data class Context(
    val policy: PolicyContext,
    val query: QueryContext = QueryContext(),
    val structuralMatches: MutableSet<StructuralMatch> = mutableSetOf(),
    val possibleVarMappings: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    val validVarMappings: MutableSet<Set<Mapping>> = mutableSetOf(),
    val filteredVariables: MutableList<String> = mutableListOf(),
    val returnedVariables: MutableList<String> = mutableListOf(),
    var inReturnSubTree: Boolean = false,
)
