package at.jku.faw.symspace.cypherrewriter.core.cypher.detector

import at.jku.faw.symspace.cypherrewriter.core.cypher.AstNode

interface PermissionDetector {
    fun process(node: AstNode): List<Detection>
}
