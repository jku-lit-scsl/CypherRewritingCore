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

import at.jku.faw.symspace.cypherrewriter.core.cypher.AstType

class PatternContext {

    private var nodeA: Node? = null
    private var rel: Relation? = null
    private var nodeB: Node? = null
    private var lastNodeB: Node? = null
    private val paths: MutableSet<Path> = mutableSetOf()

    fun getPaths(): Set<Path> {
        finished()
        return paths
    }

    fun add(element: Node) {
        if (nextIsNodeA(element)) {
            nodeA = element
        } else if (nextIsNodeB(element)) {
            nodeB = element
        } else {
            throw IllegalStateException("Error in path evaluation: Path malformed.")
        }
        updatePaths()
    }

    fun add(element: Relation) {
        if(nextIsAdditionalRelation(element)) {
            nodeA = lastNodeB
            rel = element
        } else if (nextIsFirstRelation(element)) {
            rel = element
        } else {
            throw IllegalStateException("Error in path evaluation: Path malformed.")
        }
        updatePaths()
    }

    private fun nextIsNodeA(element: Any): Boolean {
        return element is Node && nodeA == null && rel == null && nodeB == null
    }

    private fun nextIsAdditionalRelation(element: Any): Boolean {
        return element is Relation && nodeA == null && rel == null && nodeB == null && lastNodeB != null
    }

    private fun nextIsFirstRelation(element: Any): Boolean {
        return element is Relation && nodeA != null && rel == null && nodeB == null
    }

    private fun nextIsNodeB(element: Any): Boolean {
        return element is Node && nodeA != null && rel != null && nodeB == null
    }

    private fun isPathEmpty(): Boolean {
        return nodeA == null && rel == null && nodeB == null
    }

    private fun updatePaths() {
        val nodeA = this.nodeA
        val rel = this.rel
        val nodeB = this.nodeB

        if (nodeA != null && rel != null && nodeB != null) {
            generatePaths(nodeA, rel, nodeB)

            this.nodeA = null
            this.rel = null
            this.lastNodeB = nodeB
            this.nodeB = null
        }
    }

    private fun generatePaths(nodeA: Node, rel: Relation, nodeB: Node) {
        when(rel.type) {
            AstType.RELATION_RIGHT -> paths.add(Path(nodeA, rel, nodeB))
            AstType.RELATION_LEFT -> paths.add(Path(nodeB, rel, nodeA))
            AstType.RELATION_BOTH -> {
                paths.add(Path(nodeA, rel, nodeB))
                paths.add(Path(nodeB, rel, nodeA))
            }
            else -> throw IllegalStateException("Relation has invalid AstType.")
        }
    }

    private fun finished() {
        val nodeA = this.nodeA
        val rel = this.rel
        val nodeB = this.nodeB

        if (nodeA != null && rel == null && nodeB == null) {
            paths.add(Path(nodeA, null, null))
        } else if (!isPathEmpty()) {
            throw IllegalStateException("Error in path evaluation: Path incomplete")
        }
    }

}
