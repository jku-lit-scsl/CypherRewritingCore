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

package at.jku.faw.symspace.cypherrewriter.core.cypher.unparser

import at.jku.faw.symspace.cypherrewriter.core.cypher.*
import org.springframework.stereotype.Component
import java.util.StringJoiner
import kotlin.text.StringBuilder

@Component
class CypherRewritingUnparserImpl : CypherRewritingUnparser {
    override fun render(astNode: AstNode): String {

        return visit(astNode)
    }

    private fun visit(element: AstNode): String {
        return when (element.type) {
            AstType.RETURN -> renderReturn(element as AstInternalNode)
            AstType.WHERE -> renderPrefix(element as AstInternalNode)
            AstType.PATTERN -> renderPattern(element as AstInternalNode)
            AstType.OPTIONAL -> renderTerminal(element as AstLeafNoValue)
            AstType.MATCH -> renderMatch(element as AstInternalNode)
            AstType.DISTINCT -> renderTerminal(element as AstLeafNoValue)
            AstType.TEMPORARY -> error("${AstType.TEMPORARY} should not be present after parsing.")
            AstType.ORDER_BY -> renderPrefix(element as AstInternalNode)
            AstType.SKIP -> renderPrefix(element as AstInternalNode)
            AstType.LIMIT -> renderPrefix(element as AstInternalNode)
            AstType.ASTERISK -> renderTerminal(element as AstLeafNoValue)
            AstType.AS -> renderBinaryOperator(element as AstInternalNode)
            AstType.VARIABLE -> renderValue(element as AstLeafValue)
            AstType.OR -> renderBinaryOperator(element as AstInternalNode)
            AstType.XOR -> renderBinaryOperator(element as AstInternalNode)
            AstType.AND -> renderBinaryOperator(element as AstInternalNode)
            AstType.NOT -> renderPrefix(element.asNode(), prefix = element.type.value!!) //TODO delimiter should probably be ""
            AstType.EXPRESSION -> TODO()
            AstType.SORT_ITEM -> renderConcatenation(element as AstInternalNode, delimiter = " ")
            AstType.ASC -> renderTerminal(element as AstLeafNoValue)
            AstType.DESC -> renderTerminal(element as AstLeafNoValue)
            AstType.QUERY -> renderConcatenation(element as AstInternalNode, delimiter = " ")
            AstType.NODE_LABEL -> renderValue(element as AstLeafValue, prefix = ":")
            AstType.PARAMETER -> renderValue(element as AstLeafValue, "$")
            AstType.PROPERTY_KEY_NAME -> renderValue(element as AstLeafValue)
            AstType.PROPERTIES -> renderConcatenation(element as AstInternalNode, ", ","{", "}")
            AstType.PROPERTY -> renderConcatenation(element as AstInternalNode, ": ")
            AstType.PROPERTY_DOT_ACCESS -> renderPropertyDotAccess(element.asNode())
            AstType.RELATION_BOTH -> renderRelationDetails(element as AstInternalNode, "-", "-")
            AstType.RELATION_LEFT -> renderRelationDetails(element as AstInternalNode, "<-", "-")
            AstType.RELATION_RIGHT -> renderRelationDetails(element as AstInternalNode, "-", "->")
            AstType.RELATION_LABEL -> renderValue(element as AstLeafValue)
            AstType.INTEGER -> renderValue(element as AstLeafValue)
            AstType.DOUBLE -> renderValue(element as AstLeafValue)
            AstType.NODE -> renderConcatenation(element as AstInternalNode, delimiter = "", prefix = "(", suffix = ")")
            AstType.RANGE_ONE_OR_MORE -> renderTerminal(element as AstLeafNoValue)
            AstType.RANGE_FROM -> renderValue(element as AstLeafValue)
            AstType.RANGE_TO -> renderValue(element as AstLeafValue)
            AstType.RANGE_EXACTLY -> renderValue(element as AstLeafValue, prefix = "*")
            AstType.STRING -> renderValue(element as AstLeafValue)
            AstType.NULL -> renderTerminal(element as AstLeafNoValue)
            AstType.FUNCTION_NAME -> renderValue(element as AstLeafValue)
            AstType.FUNCTION_INVOCATION -> renderFunctionInvocation(element as AstInternalNode)
            AstType.ARGUMENT -> renderConcatenation(element as AstInternalNode, delimiter = ".")
            AstType.GROUP -> renderConcatenation(element.asNode(), prefix = "(", suffix = ")")
            AstType.STRUCTURAL_GROUP -> renderConcatenation(element.asNode())
            AstType.COMPARISON -> renderValue(element.asValue(), prefix = " ", suffix = " ")
        }
    }

    private fun renderMatch(astNode: AstInternalNode): String {
        val sb = StringBuilder()

        astNode.elements.find { it.type == AstType.OPTIONAL }?.let { sb.append(visit(it)).append(" ") }
        sb.append("MATCH ")
        val patternPart = astNode.elements.filter { it.type == AstType.PATTERN }.joinToString(separator = ", ") { visit(it) }
        val otherPart = astNode.elements.filter { it.type != AstType.OPTIONAL && it.type != AstType.PATTERN }.joinToString(separator = " ") { visit(it) }
        arrayOf(patternPart, otherPart).filter { it.isNotEmpty() }.joinTo(sb, separator = " ")
        return sb.toString()
    }

    private fun renderRelationDetails(astNode: AstInternalNode, prefix: String, suffix: String): String {
        val sb = StringBuilder()
        sb.append(prefix)

        val hasDetails = astNode.elements.isNotEmpty()
        if (hasDetails) {
            sb.append("[")
        }

        astNode.elements.find { it.type== AstType.VARIABLE }?.let { sb.append(visit(it)) }
        astNode.elements.filter { it.type == AstType.RELATION_LABEL }
            .ifEmpty { null }
            ?.joinToString(separator = "|", prefix = ":", transform = {visit(it)})
            ?.let { sb.append(it) }
        astNode.elements.find { it.type== AstType.RANGE_ONE_OR_MORE || it.type== AstType.RANGE_EXACTLY }?.let { sb.append(visit(it)) }
        val from = astNode.elements.find { it.type== AstType.RANGE_FROM }
        val to = astNode.elements.find { it.type== AstType.RANGE_TO }
        if (from != null || to != null) {
            sb.append("*")
            from?.let { sb.append(visit(it)) }
            sb.append("..")
            to?.let { sb.append(visit(it)) }
        }

        // TODO: handle properties

        if (hasDetails) {
            sb.append("]")
        }
        sb.append(suffix)
        return sb.toString()
    }

    private fun renderBinaryOperator(astNode: AstInternalNode): String {
        val joiner = StringJoiner(" ${astNode.type} ")
        astNode.elements.forEach { joiner.add(visit(it)) }
        return joiner.toString()
    }

    private fun renderTerminal(astLeafNoValue: AstLeafNoValue): String {
        return astLeafNoValue.type.value!!
    }

    private fun renderValue(astValue: AstLeafValue, prefix: String = "", suffix: String = ""): String {
        return prefix + astValue.value.toString() + suffix
    }

    private fun renderConcatenation(astNode: AstInternalNode, delimiter: String = "", prefix: String = "", suffix: String = ""): String {
        val joiner = StringJoiner(delimiter, prefix, suffix)
        astNode.elements.forEach { joiner.add(visit(it)) }
        return joiner.toString()
    }

    private fun renderPattern(astNode: AstInternalNode): String {
        val sb = StringBuilder()

        val varNode = astNode.elements.find { it.type== AstType.VARIABLE }
        if (varNode != null) {
            sb.append(visit(varNode))
            sb.append("=")
            astNode.elements.filter { it.type!= AstType.VARIABLE }.forEach { sb.append(visit(it)) }
        } else {
            astNode.elements.forEach { sb.append(visit(it)) }
        }

        return sb.toString()
    }

    private fun renderPrefix(node: AstInternalNode, prefix: String = node.type.value!!, delimiter: String = ", "): String {
        val sb = StringJoiner(delimiter, "$prefix ", "")
        node.elements.forEach { sb.add(visit(it)) }
        return sb.toString()
    }

    private fun renderReturn(node: AstInternalNode): String {
        val sb = StringBuilder("RETURN ")
        node.elements.find { it.type == AstType.DISTINCT }?.let { sb.append(render(it)).append(" ") }
        node.elements.filter { it.type !in arrayOf(AstType.DISTINCT, AstType.ORDER_BY, AstType.SKIP, AstType.LIMIT) }.joinTo(sb, separator = ", ", transform = {visit(it)})
        node.elements.filter { it.type in arrayOf(AstType.ORDER_BY, AstType.SKIP, AstType.LIMIT) }.joinTo(sb, separator = " ", transform = {visit(it)})
        return sb.toString()
    }

    private fun renderNode(node: AstInternalNode): String {
        val sb = StringBuilder("(")
        node.elements.find { it.type == AstType.VARIABLE }?.let { visit(it) }?.let { sb.append(it) }
        node.elements.filter { it.type == AstType.NODE_LABEL }.ifEmpty { null }?.joinTo(sb, prefix = ":", separator = ":", transform = {visit(it)})
        node.elements.filter { it.type == AstType.PROPERTIES }.forEach { sb.append(visit(it)) }
        sb.append(")")
        return sb.toString()
    }

    private fun renderFunctionInvocation(node: AstInternalNode): String {
        val sb = StringBuilder()
        node.elements.find { it.type == AstType.FUNCTION_NAME }?.let { sb.append(visit(it)) }
        sb.append("(")
        node.elements.find { it.type == AstType.DISTINCT }?.let { sb.append(visit(it)).append(" ") }
        node.elements.filter { it.type !in setOf(AstType.FUNCTION_NAME, AstType.DISTINCT) }.joinTo(sb, separator = ", ") { visit(it) }
        sb.append(")")
        return sb.toString()
    }

    private fun renderPropertyDotAccess(node: AstInternalNode): String {
        if (node.elements.size == 1) {
            return visit(node.elements.first())
        }

        val sb = StringBuilder()
        node.elements.forEach {
            val delimiter = when(it.type) {
                AstType.VARIABLE -> ""
                AstType.NODE_LABEL -> ""
                AstType.RELATION_LABEL -> ":"
                else -> "."
            }
            sb.append(delimiter).append(visit(it))
        }
        return sb.toString()
    }


}
