package at.jku.faw.symspace.cypherrewriter.core.cypher.detector

import at.jku.faw.symspace.cypherrewriter.core.cypher.*
import org.springframework.stereotype.Component

@Component
class NodeDetector(permissionPolicy: PermissionPolicy) : SubDetector {
    private val rules = permissionPolicy.rules.filter { it.ressource.type == AstType.NODE }
    private val labelRuleMap: MutableMap<String, MutableList<PermissionRule>> = mutableMapOf()

    init {
        rules.forEach{rule ->
            val labels = rule.subjectNode.asNode().elements.filter { n -> n.type == AstType.NODE_LABEL }.map { labelNode -> labelNode.asValue().value.toString() }
            labels.forEach { labelRuleMap.computeIfAbsent(it){ mutableListOf() }.add(rule) }
        }
    }

    override fun process(ast: AstElement): List<Detection> {
        val ctx = Context()
        extractInformation(ctx, ast)
        return ctx.detections
    }

    private fun traverse(ctx: Context, ast: AstElement) {
        if (ast is AstNode) {
            ast.elements.forEach { extractInformation(ctx, it) }
        }
    }

    private fun extractInformation(ctx: Context, element: AstElement) {
        when(element.type) {
            AstType.NODE -> processNode(ctx, element as AstNode)
            AstType.NODE_LABEL -> processNodeLabel(ctx, element as AstValue)
            AstType.MATCH -> processMatch(ctx, element as AstNode)
            AstType.PROPERTIES -> ctx.currentNodeCtx?.filtered = true
            else -> traverse(ctx, element)
        }
    }

    private fun processMatch(ctx: Context, astNode: AstNode) {
        ctx.currentMatch = astNode
        traverse(ctx, astNode)
        ctx.currentMatch = null
    }


    private fun processNode(ctx: Context, astNode: AstNode) {
        val nodeCtx = NodeContext()
        ctx.currentNodeCtx = nodeCtx
        traverse(ctx, astNode)
        nodeCtx.labels.forEach { label ->
            labelRuleMap[label]?.forEach {
                val detection = Detection(it)
                detection.protectedNode = astNode
                detection.enforcementNode = ctx.currentMatch
                if (nodeCtx.filtered) {
                    detection.filterType = FilterType.FILTERED
                }
                ctx.detections.add(detection)
            }
        }

    }

    private fun processNodeLabel(ctx: Context, value: AstValue) {
        ctx.currentNodeCtx?.labels?.add(value.value.toString())
    }


    private class Context {
        val detections = mutableListOf<Detection>()
        var currentMatch: AstNode? = null
        var currentNodeCtx: NodeContext? = null
    }

    private class NodeContext {
        val labels = mutableListOf<String>()
        var filtered = false
    }



}
