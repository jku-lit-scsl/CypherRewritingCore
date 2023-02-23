package at.jku.faw.symspace.cypherrewriter.core.cypher.renderer

import at.jku.faw.symspace.cypherrewriter.core.cypher.*
import org.springframework.stereotype.Component

@Component
class AstVisualizerImpl : AstVisualizer {

    private val indentation: String = "    "
    override fun render(astElement: AstElement): String {
        return render(astElement, 0)
    }

    private fun render(astElement: AstElement, level: Int): String {
        return when (astElement) {
            is AstNode -> renderNode(astElement, level)
            is AstValue -> renderValue(astElement, level)
            is AstTerminal -> renderTerminal(astElement, level)
        }
    }

    private fun renderTerminal(astTerminal: AstTerminal, level: Int): String {
        return getIndentation(level) + astTerminal.type.name + "\n"
    }

    private fun renderValue(astValue: AstValue, level: Int): String {
        return getIndentation(level) + astValue.type.name + " = " + astValue.value + "\n"
    }

    private fun renderNode(astNode: AstNode, level: Int): String {
        return getIndentation(level) + astNode.type.name + " {\n" +
                astNode.elements.joinToString(separator = "") { render(it, level + 1) } +
                getIndentation(level) + "}\n"
    }

    private fun getIndentation(level: Int): String {
        return indentation.repeat(level)
    }


}
