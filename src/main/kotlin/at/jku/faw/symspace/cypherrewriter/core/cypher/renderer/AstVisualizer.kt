package at.jku.faw.symspace.cypherrewriter.core.cypher.renderer

import at.jku.faw.symspace.cypherrewriter.core.cypher.AstElement

interface AstVisualizer {
    fun render(astElement: AstElement): String
    fun print(astElement: AstElement) {
        println(render(astElement))
    }
}
