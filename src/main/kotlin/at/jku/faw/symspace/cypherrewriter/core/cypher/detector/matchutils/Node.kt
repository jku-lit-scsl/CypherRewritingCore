package at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils

data class Node(val variable: String, val labels: Set<String>) {
    override fun toString(): String {
        return "($variable:${labels.joinToString(":")})"
    }
}
