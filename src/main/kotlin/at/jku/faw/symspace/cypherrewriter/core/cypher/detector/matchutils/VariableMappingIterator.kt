package at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils

import java.lang.IllegalStateException

class VariableMappingIterator(val items: List<MappingItem>) : Iterator<Set<Mapping>> {

    var next = nextValue()
    private var increaseNext = false
    private var wasIllegalMapping = false
    override fun hasNext(): Boolean {
        return next != null
    }

    override fun next(): Set<Mapping> {
        val returnValue = next
        generateNewNext()
        if (returnValue != null) {
            return returnValue
        } else {
            throw IllegalStateException("Iterator is empty.")
        }
    }

    private fun generateNewNext() {
        var newNext: Set<Mapping>?
        do {
            wasIllegalMapping = false
            newNext = nextValue()
        } while (wasIllegalMapping)
        next = newNext
    }

    private fun nextValue(): Set<Mapping>? {
        val mappings = mutableSetOf<Mapping>()

        for (item in items) {
            if (increaseNext) {
                if (item.isEmpty()) {
                    increaseNext = true
                    item.reset()
                } else {
                    increaseNext = false
                    item.next()
                }
            }

            val mapping = item.get()
            if (mappings.any { it.policyVar != mapping.policyVar && it.queryVar == mapping.queryVar }) {
                wasIllegalMapping = true
                increaseNext = true
                return null
            }
            mappings.add(mapping)
        }

        if (increaseNext) {
            return null
        }
        increaseNext = true

        if (mappings.isEmpty()) {
            return null
        }
        return mappings
    }

    companion object {
        fun of(possibleMappings: Map<String, Set<String>>): VariableMappingIterator {
            val items = mutableListOf<MappingItem>()
            for ((key, values) in possibleMappings) {
                items.add(MappingItem(key, values))
            }
            return VariableMappingIterator(items)
        }
    }
}
