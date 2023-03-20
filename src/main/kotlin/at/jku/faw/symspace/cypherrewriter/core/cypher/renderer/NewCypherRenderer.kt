package at.jku.faw.symspace.cypherrewriter.core.cypher.renderer

import at.jku.faw.symspace.cypherrewriter.core.cypher.AstNode

interface NewCypherRenderer {
    fun render(astNode: AstNode): String
}
