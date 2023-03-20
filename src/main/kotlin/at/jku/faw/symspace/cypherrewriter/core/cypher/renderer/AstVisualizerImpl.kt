package at.jku.faw.symspace.cypherrewriter.core.cypher.renderer

import at.jku.faw.symspace.cypherrewriter.core.cypher.*
import org.springframework.stereotype.Component

@Component
class AstVisualizerImpl : AstVisualizer {

    private val indentation: String = "    "
    override fun render(astNode: AstNode): String {
        return render(astNode, 0)
    }

    private fun render(astNode: AstNode, level: Int): String {
        return when (astNode) {
            is AstInternalNode -> renderNode(astNode, level)
            is AstLeafValue -> renderValue(astNode, level)
            is AstLeafNoValue -> renderTerminal(astNode, level)
        }
    }

    private fun renderTerminal(astLeafNoValue: AstLeafNoValue, level: Int): String {
        return getIndentation(level) + astLeafNoValue.type.name + "\n"
    }

    private fun renderValue(astValue: AstLeafValue, level: Int): String {
        return getIndentation(level) + astValue.type.name + " = " + astValue.value + "\n"
    }

    private fun renderNode(astNode: AstInternalNode, level: Int): String {
        return getIndentation(level) + astNode.type.name + " {\n" +
                astNode.elements.joinToString(separator = "") { render(it, level + 1) } +
                getIndentation(level) + "}\n"
    }

    private fun getIndentation(level: Int): String {
        return indentation.repeat(level)
    }


}
