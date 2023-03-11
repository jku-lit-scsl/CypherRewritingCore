package at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils

import at.jku.faw.symspace.cypherrewriter.core.cypher.AstNode

data class Variable(val name: String, val definingElement: AstNode, val knownLabels: MutableSet<String> = mutableSetOf(), val state: VariableState = VariableState()) {
}
