package at.jku.faw.symspace.cypherrewriter.core.cypher.detector

import at.jku.faw.symspace.cypherrewriter.core.cypher.AstNode
import at.jku.faw.symspace.cypherrewriter.core.cypher.FilterType
import at.jku.faw.symspace.cypherrewriter.core.cypher.PermissionRule
import at.jku.faw.symspace.cypherrewriter.core.cypher.ReturnType

class Detection(val rule: PermissionRule) {
    var protectedNode: AstNode? = null
    var returnType: ReturnType? = null
    var filterType: FilterType? = null
    var enforcementNode: AstNode? = null
    val authorizationLevel get()= rule.levels.get(returnType, filterType)
}
