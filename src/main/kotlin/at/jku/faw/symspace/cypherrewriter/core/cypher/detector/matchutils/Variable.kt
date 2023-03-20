package at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils

import at.jku.faw.symspace.cypherrewriter.core.cypher.AstInternalNode

data class Variable(val name: String, val definingElement: AstInternalNode, val knownLabels: MutableSet<String> = mutableSetOf(), val state: VariableState = VariableState()) {
}
