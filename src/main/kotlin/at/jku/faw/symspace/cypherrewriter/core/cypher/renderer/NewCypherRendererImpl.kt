package at.jku.faw.symspace.cypherrewriter.core.cypher.renderer

import at.jku.faw.symspace.cypherrewriter.core.cypher.*
import org.springframework.stereotype.Component
import java.util.StringJoiner
import kotlin.text.StringBuilder

@Component
class NewCypherRendererImpl : NewCypherRenderer {
    override fun render(astElement: AstElement): String {

        return visit(astElement)
    }

    private fun visit(element: AstElement): String {
        return when (element.type) {
            AstType.RETURN -> renderReturn(element as AstNode)
            AstType.WHERE -> renderPrefix(element as AstNode)
            AstType.PATTERN -> renderPattern(element as AstNode)
            AstType.OPTIONAL -> renderTerminal(element as AstTerminal)
            AstType.MATCH -> renderMatch(element as AstNode)
            AstType.DISTINCT -> renderTerminal(element as AstTerminal)
            AstType.TEMPORARY -> error("${AstType.TEMPORARY} should not be present after parsing.")
            AstType.ORDER_BY -> renderPrefix(element as AstNode)
            AstType.SKIP -> renderPrefix(element as AstNode)
            AstType.LIMIT -> renderPrefix(element as AstNode)
            AstType.ASTERISK -> renderTerminal(element as AstTerminal)
            AstType.AS -> renderBinaryOperator(element as AstNode)
            AstType.VARIABLE -> renderValue(element as AstValue)
            AstType.OR -> renderBinaryOperator(element as AstNode)
            AstType.XOR -> renderBinaryOperator(element as AstNode)
            AstType.AND -> renderBinaryOperator(element as AstNode)
            AstType.NOT -> renderPrefix(element.asNode(), prefix = element.type.value!!)
            AstType.EXPRESSION -> TODO()
            AstType.SORT_ITEM -> renderConcatenation(element as AstNode, delimiter = " ")
            AstType.ASC -> renderTerminal(element as AstTerminal)
            AstType.DESC -> renderTerminal(element as AstTerminal)
            AstType.QUERY -> renderConcatenation(element as AstNode, delimiter = " ")
            AstType.NODE_LABEL -> renderValue(element as AstValue)
            AstType.PARAMETER -> renderValue(element as AstValue, "$")
            AstType.PROPERTY_KEY_NAME -> renderValue(element as AstValue)
            AstType.PROPERTIES -> renderConcatenation(element as AstNode, ", ","{", "}")
            AstType.PROPERTY -> renderConcatenation(element as AstNode, ": ")
            AstType.PROPERTY_DOT_ACCESS -> renderConcatenation(element.asNode(), ".")
            AstType.RELATION_BOTH -> renderRelationDetails(element as AstNode, "-", "-")
            AstType.RELATION_LEFT -> renderRelationDetails(element as AstNode, "<-", "-")
            AstType.RELATION_RIGHT -> renderRelationDetails(element as AstNode, "-", "->")
            AstType.RELATION_LABEL -> renderValue(element as AstValue)
            AstType.INTEGER -> renderValue(element as AstValue)
            AstType.DOUBLE -> renderValue(element as AstValue)
            AstType.NODE -> renderNode(element as AstNode)
            AstType.RANGE_ONE_OR_MORE -> renderTerminal(element as AstTerminal)
            AstType.RANGE_FROM -> renderValue(element as AstValue)
            AstType.RANGE_TO -> renderValue(element as AstValue)
            AstType.RANGE_EXACTLY -> renderValue(element as AstValue, prefix = "*")
            AstType.STRING -> renderValue(element as AstValue)
            AstType.NULL -> renderTerminal(element as AstTerminal)
            AstType.FUNCTION_NAME -> renderValue(element as AstValue)
            AstType.FUNCTION_INVOCATION -> renderFunctionInvocation(element as AstNode)
            AstType.ARGUMENT -> renderConcatenation(element as AstNode, delimiter = ".")
            AstType.GROUP -> renderConcatenation(element.asNode(), prefix = "(", suffix = ")")
            AstType.STRUCTURAL_GROUP -> renderConcatenation(element.asNode())
            AstType.COMPARISON -> renderValue(element.asValue(), prefix = " ", suffix = " ")
        }
    }

    private fun renderMatch(astNode: AstNode): String {
        val sb = StringBuilder()

        astNode.elements.find { it.type == AstType.OPTIONAL }?.let { sb.append(visit(it)).append(" ") }
        sb.append("MATCH ")
        astNode.elements.filter { it.type == AstType.PATTERN }.joinTo(sb, separator = ", ") { visit(it) }
        astNode.elements.filter { it.type != AstType.OPTIONAL && it.type != AstType.PATTERN }.joinTo(sb, separator = " ", prefix = " ") { visit(it) }
        return sb.toString()
    }

    private fun renderRelationDetails(astNode: AstNode, prefix: String, suffix: String): String {
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

        if (hasDetails) {
            sb.append("]")
        }
        sb.append(suffix)
        return sb.toString()
    }

    private fun renderBinaryOperator(astNode: AstNode): String {
        val joiner = StringJoiner(" ${astNode.type} ")
        astNode.elements.forEach { joiner.add(visit(it)) }
        return joiner.toString()
    }

    private fun renderTerminal(astTerminal: AstTerminal): String {
        return astTerminal.type.value!!
    }

    private fun renderValue(astValue: AstValue, prefix: String = "", suffix: String = ""): String {
        return prefix + astValue.value.toString() + suffix
    }

    private fun renderConcatenation(astNode: AstNode, delimiter: String = "", prefix: String = "", suffix: String = ""): String {
        val joiner = StringJoiner(delimiter, prefix, suffix)
        astNode.elements.forEach { joiner.add(visit(it)) }
        return joiner.toString()
    }

    private fun renderPattern(astNode: AstNode): String {
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

    private fun renderPrefix(node: AstNode, prefix: String = node.type.value!!, delimiter: String = ", "): String {
        val sb = StringJoiner(delimiter, "$prefix ", "")
        node.elements.forEach { sb.add(visit(it)) }
        return sb.toString()
    }

    private fun renderReturn(node: AstNode): String {
        val sb = StringBuilder("RETURN ")
        node.elements.find { it.type == AstType.DISTINCT }?.let { sb.append(render(it)).append(" ") }
        node.elements.filter { it.type !in arrayOf(AstType.DISTINCT, AstType.ORDER_BY, AstType.SKIP, AstType.LIMIT) }.joinTo(sb, separator = ", ", postfix = " ", transform = {visit(it)})
        node.elements.filter { it.type in arrayOf(AstType.ORDER_BY, AstType.SKIP, AstType.LIMIT) }.joinTo(sb, separator = " ", transform = {visit(it)})
        return sb.toString()
    }

    private fun renderNode(node: AstNode): String {
        val sb = StringBuilder("(")
        node.elements.find { it.type == AstType.VARIABLE }?.let { visit(it) }.let { sb.append(it) }
        node.elements.filter { it.type == AstType.NODE_LABEL }.ifEmpty { null }?.joinTo(sb, prefix = ":", separator = ":", transform = {visit(it)})
        node.elements.filter { it.type == AstType.PROPERTIES }.forEach { sb.append(visit(it)) }
        sb.append(")")
        return sb.toString()
    }

    private fun renderFunctionInvocation(node: AstNode): String {
        val sb = StringBuilder()
        node.elements.find { it.type == AstType.FUNCTION_NAME }?.let { sb.append(visit(it)) }
        sb.append("(")
        node.elements.find { it.type == AstType.DISTINCT }?.let { sb.append(visit(it)).append(" ") }
        node.elements.filter { it.type !in setOf(AstType.FUNCTION_NAME, AstType.DISTINCT) }.joinTo(sb, separator = ", ") { visit(it) }
        sb.append(")")
        return sb.toString()
    }


}
