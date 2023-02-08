package at.jku.faw.symspace.cypherrewriter.core.cypher.detector

import at.jku.faw.symspace.cypherrewriter.core.cypher.AstElement

interface SubDetector {
    fun process(ast: AstElement): List<Detection>
}
