package at.jku.faw.symspace.cypherrewriter.core.cypher.detector

import at.jku.faw.symspace.cypherrewriter.core.cypher.*

class Detection(val rule: Rule) {
    var protectedNode: AstNode? = null
    var returnType: ReturnType? = null
    var filterType: FilterType? = null
    var enforcementNode: AstNode? = null
    val authorizationLevel get()= rule.authorizationLevel
}
