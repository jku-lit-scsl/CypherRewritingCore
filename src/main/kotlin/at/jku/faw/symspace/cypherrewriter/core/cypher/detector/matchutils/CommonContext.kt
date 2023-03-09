package at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils

open class CommonContext(
    val paths: MutableSet<Path> = mutableSetOf(),
    val nodes: MutableSet<Node> = mutableSetOf(),
    val varToLabels: MutableMap<String, MutableSet<String>> = mutableMapOf(),
) {
    open val evaluateQuerySpecifics = false
    fun asQueryCtx(): QueryContext { return this as QueryContext }
    fun asPolicyCtx(): PolicyContext { return this as PolicyContext }

}
