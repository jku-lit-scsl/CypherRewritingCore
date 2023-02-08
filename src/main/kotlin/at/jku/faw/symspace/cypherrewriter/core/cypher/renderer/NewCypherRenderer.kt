package at.jku.faw.symspace.cypherrewriter.core.cypher.renderer

import at.jku.faw.symspace.cypherrewriter.core.cypher.AstElement

interface NewCypherRenderer {
    fun render(astElement: AstElement): String
}
