package at.jku.faw.symspace.cypherrewriter.core.cypher.renderer

import at.jku.faw.symspace.cypherrewriter.core.cypher.AstNode

interface AstVisualizer {
    fun render(astNode: AstNode): String
    fun print(astNode: AstNode) {
        println(render(astNode))
    }
}
