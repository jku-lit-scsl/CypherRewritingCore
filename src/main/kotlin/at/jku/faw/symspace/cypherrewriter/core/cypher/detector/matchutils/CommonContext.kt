package at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils

import at.jku.faw.symspace.cypherrewriter.core.cypher.AstInternalNode

open class CommonContext(
    val paths: MutableSet<Path> = mutableSetOf(),
    val nodes: MutableSet<Node> = mutableSetOf(),
    val relations: MutableSet<Relation> = mutableSetOf(),
    val variables: MutableSet<Variable> = mutableSetOf()
) {
    var parentNode: AstInternalNode? = null
    open val evaluateQuerySpecifics = false
    var patternContext: PatternContext? = null
}
