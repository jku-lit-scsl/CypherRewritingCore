package at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils

class MappingItem(private val policyVar: Variable, private val dests: Iterable<Variable>) {
    private var iterator = dests.iterator()
    private var queryVar = iterator.next()

    fun isEmpty(): Boolean {
        return !iterator.hasNext()
    }

    fun get(): Mapping {
        return Mapping(policyVar, queryVar)
    }

    fun next() {
        queryVar = iterator.next()
    }

    fun reset() {
        iterator = dests.iterator()
        next()
    }
}
