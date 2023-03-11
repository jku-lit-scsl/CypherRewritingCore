package at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils

import at.jku.faw.symspace.cypherrewriter.core.cypher.AstNode

class QueryContext(): CommonContext() {
    override val evaluateQuerySpecifics = true

    var isReturnClause: Boolean = false
    var isFiltered: Boolean = false
    var isAggregationFunction: Boolean = false
    var lastVar: Variable? = null
    var isWhereClause: Boolean = false
    var matchClause: AstNode? = null
}
