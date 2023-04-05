package at.jku.faw.symspace.cypherrewriter.core.cypher.enforcer

import at.jku.faw.symspace.cypherrewriter.core.cypher.*
import at.jku.faw.symspace.cypherrewriter.core.cypher.detector.Detection
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class CypherEnforcerImpl(private val appContext: CypherAppContext, private val permissionConfig: PermissionConfig) :
    CypherEnforcer {
    override fun enforce(detections: Collection<Detection>) {
        detections.forEach { enforce(it) }
    }

    private fun enforce(detection: Detection) {
        val authLevel = detection.rule.authorizationLevel
        if (authLevel == AuthorizationLevel.PUBLIC_LEVEL) {
            return
        }

        val protectedVar = detection.protectedNode?.let { getVariable(it) }?.value.toString()
        val whereNode = detection.enforcementNode?.let { getWhere(it) } ?: return
        val filter = getFilter(detection.rule)
        val filterString = getFilterString(filter, protectedVar)


        val filterAst = AstLeafValue(AstType.STRING, filterString)
        if (whereNode.elements.isEmpty()) {
            whereNode.elements.add(filterAst)
        } else {
            val andNode = AstInternalNode(AstType.AND)
            andNode.elements.add(filterAst)
            andNode.elements.addAll(whereNode.elements) //TODO add support for parenthesis
            whereNode.elements.clear()
            whereNode.elements.add(andNode)
        }
    }

    private fun getFilter(rule: Rule): FilterTemplate {
        return permissionConfig.filterTemplates.find { it.filterId == rule.filterId && it.authorizationLevel == rule.authorizationLevel }
            ?: throw IllegalStateException("The filter ${rule.filterId} with authorization level ${rule.authorizationLevel} was not defined.")
    }

    private fun getFilterString(filterTemplate: FilterTemplate, resourceVariableName: String): String {
        val args = filterTemplate.arguments.map {
            when (it) {
                ArgumentType.USERNAME -> appContext.currentUsername
                ArgumentType.RESOURCE_VARIABLE -> resourceVariableName
            }
        }
        return filterTemplate.pattern.format(*args.toTypedArray())
    }


    private fun getWhere(enforcementNode: AstInternalNode): AstInternalNode {
        var whereNode = enforcementNode.elements.find { it.type == AstType.WHERE }?.asNode()
        if (whereNode == null) {
            whereNode = AstInternalNode(AstType.WHERE)
            enforcementNode.elements.add(whereNode)
        }
        return whereNode
    }

    private fun getVariable(astNode: AstInternalNode): AstLeafValue {
        var varNode = astNode.elements.find { it.type == AstType.VARIABLE }?.asValue()
        if (varNode == null) {
            varNode = AstLeafValue(AstType.VARIABLE, "_genVar${Random.nextInt(0, Int.MAX_VALUE)}")
            astNode.elements.add(varNode)
        }
        return varNode
    }
}
