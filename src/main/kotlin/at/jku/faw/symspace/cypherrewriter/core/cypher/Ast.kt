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

package at.jku.faw.symspace.cypherrewriter.core.cypher

sealed class AstNode(val type: AstType) {


    abstract fun copy(overrideType: AstType = this.type): AstNode
    fun asNode(): AstInternalNode {
        return this as AstInternalNode
    }

    fun asValue(): AstLeafValue {
        return this as AstLeafValue
    }
}

class AstInternalNode(type: AstType) : AstNode(type) {
    val elements: MutableList<AstNode> = mutableListOf()

    override fun copy(overrideType: AstType): AstInternalNode {
        return AstInternalNode(overrideType).also { copy ->
            this.elements.map { it.copy() }.forEach { copy.elements.add(it) }
        }
    }
}

class AstLeafValue(type: AstType, val value: Any) : AstNode(type) {
    override fun copy(overrideType: AstType): AstLeafValue {
        return AstLeafValue(overrideType, value)
    }
}

class AstLeafNoValue(type: AstType) : AstNode(type) {
    override fun copy(overrideType: AstType): AstLeafNoValue {
        return AstLeafNoValue(overrideType)
    }
}

enum class AstType(val value: String? = null) {
    RETURN("RETURN"),
    WHERE("WHERE"),
    PATTERN("pattern"),
    OPTIONAL("OPTIONAL"),
    MATCH("MATCH"),
    DISTINCT("DISTINCT"),
    TEMPORARY,
    ORDER_BY("ORDER BY"),
    SKIP("SKIP"),
    LIMIT("LIMIT"),
    ASTERISK("*"),
    AS("AS"),
    VARIABLE,
    OR("OR"),
    XOR("XOR"),
    AND("AND"),
    NOT("NOT"),
    GROUP,
    STRUCTURAL_GROUP,
    EXPRESSION,
    SORT_ITEM,
    ASC("ASC"),
    DESC("DESC"),
    QUERY,
    NODE_LABEL,
    PARAMETER,
    PROPERTY_KEY_NAME,
    PROPERTIES,
    PROPERTY,
    PROPERTY_DOT_ACCESS,
    RELATION_BOTH,
    RELATION_LEFT,
    RELATION_RIGHT,
    RELATION_LABEL,
    INTEGER,
    DOUBLE,
    STRING,
    NODE,
    RANGE_EXACTLY,
    RANGE_ONE_OR_MORE("*"),
    RANGE_FROM,
    RANGE_TO,
    NULL("NULL"),
    FUNCTION_INVOCATION,
    FUNCTION_NAME,
    ARGUMENT,
    COMPARISON,
}

object AstMetadata {
    val SET_OF_VARIABLE_STORABLE_AST_TYPES = setOf(
        AstType.NODE,
        AstType.RELATION_BOTH,
        AstType.RELATION_RIGHT,
        AstType.RELATION_LEFT,
        AstType.PATTERN
    )
}
