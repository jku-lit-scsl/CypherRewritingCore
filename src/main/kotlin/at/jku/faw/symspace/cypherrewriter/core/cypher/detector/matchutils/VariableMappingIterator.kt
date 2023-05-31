/*
 * Cypher Rewriting System
 * Copyright (C) 2023 Daniel Hofer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils

import java.lang.IllegalStateException

class VariableMappingIterator(private val items: List<MappingItem>) : Iterator<Set<Mapping>> {

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
        fun of(possibleMappings: Map<Variable, Set<Variable>>): VariableMappingIterator {
            val items = mutableListOf<MappingItem>()
            for ((key, values) in possibleMappings) {
                items.add(MappingItem(key, values))
            }
            return VariableMappingIterator(items)
        }
    }
}
