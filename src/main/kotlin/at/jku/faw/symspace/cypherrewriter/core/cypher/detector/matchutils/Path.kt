package at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils

data class Path(val from: Node, val relationship: Relation?, val to: Node?) {
    override fun toString(): String {
        return "$from-$relationship->$to"
    }
}
