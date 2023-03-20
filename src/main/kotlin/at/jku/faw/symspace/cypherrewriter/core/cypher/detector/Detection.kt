package at.jku.faw.symspace.cypherrewriter.core.cypher.detector

import at.jku.faw.symspace.cypherrewriter.core.cypher.*

class Detection(val rule: Rule) {
    var protectedNode: AstInternalNode? = null
    var enforcementNode: AstInternalNode? = null
    val authorizationLevel get()= rule.authorizationLevel
}
