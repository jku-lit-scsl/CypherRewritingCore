package at.jku.faw.symspace.cypherrewriter.core.cypher.enforcer

import at.jku.faw.symspace.cypherrewriter.core.cypher.*
import at.jku.faw.symspace.cypherrewriter.core.cypher.detector.Detection
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class CypherEnforcerImpl(private val appContext: CypherAppContext) : CypherEnforcer {
    override fun enforce(detections: Collection<Detection>) {
        detections.forEach { enforce(it) }
    }

    private fun enforce(detection: Detection) {
        val authLevel = detection.rule.levels.get(detection.returnType, detection.filterType)
        if (authLevel == AuthorizationLevel.PUBLIC_LEVEL) {
            return
        }

        val protectedVar = detection.protectedNode?.let { getVariable(it) } ?: return
        val whereNode = detection.enforcementNode?.let { getWhere(it) } ?: return
        val filterString =
            detection.rule.enforcementFilter.let {
                getFilterString(
                    it,
                    detection.rule.arguments,
                    protectedVar.value.toString()
                )
            }


        val filterAst = AstValue(AstType.STRING, filterString)
        if (whereNode.elements.isEmpty()) {
            whereNode.elements.add(filterAst)
        } else {
            val andNode = AstNode(AstType.AND)
            andNode.elements.add(filterAst)
            andNode.elements.addAll(whereNode.elements) //TODO add support for parenthesis
            whereNode.elements.clear()
            whereNode.elements.add(andNode)
        }
    }

    private fun getFilterString(
        enforcementFilter: String,
        filterArguments: List<ArgumentType>,
        variable: String
    ): String {
        val args = filterArguments.map {
            when (it) {
                ArgumentType.USERNAME -> appContext.currentUsername
                ArgumentType.RESOURCE_VARIABLE -> variable
            }
        }
        return enforcementFilter.format(*args.toTypedArray())
    }


    private fun getWhere(enforcementNode: AstNode): AstNode {
        var whereNode = enforcementNode.elements.find { it.type == AstType.WHERE }?.asNode()
        if (whereNode == null) {
            whereNode = AstNode(AstType.WHERE)
            enforcementNode.elements.add(whereNode)
        }
        return whereNode
    }

    private fun getVariable(astNode: AstNode): AstValue {
        var varNode = astNode.elements.find { it.type == AstType.VARIABLE }?.asValue()
        if (varNode == null) {
            varNode = AstValue(AstType.VARIABLE, "_genVar${Random.nextInt(0, Int.MAX_VALUE)}")
            astNode.elements.add(varNode)
        }
        return varNode
    }
}
