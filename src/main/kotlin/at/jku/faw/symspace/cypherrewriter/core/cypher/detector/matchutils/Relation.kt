package at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils

import at.jku.faw.symspace.cypherrewriter.core.cypher.AstType

data class Relation(val variable: Variable, val type: AstType, val labels: Set<String>) {
    override fun toString(): String {
        return "[$variable:${labels.joinToString(":")}]"
    }
}
