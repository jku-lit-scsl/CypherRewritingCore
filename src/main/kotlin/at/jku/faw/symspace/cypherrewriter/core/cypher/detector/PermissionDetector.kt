package at.jku.faw.symspace.cypherrewriter.core.cypher.detector

import at.jku.faw.symspace.cypherrewriter.core.cypher.AstInternalNode

interface PermissionDetector {
    fun process(node: AstInternalNode): List<Detection>
}
