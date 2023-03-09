package at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils

class QueryContext(
    val varStates: MutableMap<String, VariableState> = mutableMapOf(),
): CommonContext() {
    override val evaluateQuerySpecifics = true

    var isReturn: Boolean = false
    var isFiltered: Boolean = false
    var isAggregated: Boolean = false
    var lastVar: String? = null
    var isWhere: Boolean = false
}
