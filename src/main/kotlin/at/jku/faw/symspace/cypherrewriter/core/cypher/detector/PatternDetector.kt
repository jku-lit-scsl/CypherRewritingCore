package at.jku.faw.symspace.cypherrewriter.core.cypher.detector

import at.jku.faw.symspace.cypherrewriter.core.cypher.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PatternDetector(@Autowired permissionPolicy: PermissionPolicy) : SubDetector {
    private final val relevantRules: List<PermissionRule> = permissionPolicy.rules.filter { it.ressource.type == AstType.PATTERN }

    private fun traverse(ctx: Context, ast: AstElement) {
        if (ast is AstNode) {
            ast.elements.forEach { extractInformation(ctx, it) }
        }
    }

    override fun process(ast: AstElement): List<Detection> {
        val ctx = Context()
        extractInformation(ctx, ast)
        return ctx.detections
    }

    private fun extractInformation(ctx: Context, ast: AstElement) {
        when (ast.type) {
            AstType.PATTERN -> processPattern(ctx, ast as AstNode)
            AstType.MATCH -> processMatch(ctx, ast as AstNode)
            else -> {
                if (ast is AstNode) {
                    traverse(ctx, ast)
                }
            }
        }
    }



    private fun processMatch(ctx: Context, astNode: AstNode) {
        ctx.currentMatch = astNode
        astNode.elements.find { it.type == AstType.WHERE }?.let { ctx.currentWhere = it as AstNode }
        traverse(ctx, astNode)
    }

    private fun processPattern(ctx: Context, astNode: AstNode) {
        val rules = relevantRules.filter { it.ressource.type == AstType.PATTERN }
        val filtered: FilterType? = if(astNode.elements.any { it.type == AstType.PROPERTIES }) {
            FilterType.FILTERED
        } else {
            null
        }
        for (rule in rules) {
            ctx.ruleSubjectNode = rule.subjectNode
            if (queryPatternMatchesPolicyPattern(ctx, astNode, rule.ressource)) {
                Detection(rule).also{
                    it.protectedNode = ctx.querySubjectNode
                    it.enforcementNode = ctx.currentMatch
                    it.filterType = filtered
                    ctx.detections.add(it)
                }
            }
        }
    }

    private fun queryPatternMatchesPolicyPattern(ctx: Context, queryPattern: AstNode, policyPattern: AstNode): Boolean {
        val lenDiff = queryPattern.elements.size - policyPattern.elements.size
        if (queryPattern.type != policyPattern.type || lenDiff < 0) {
            return false
        }

        val inversePolicyPattern = inversePattern(policyPattern)
        for (offset in 0..lenDiff) {
            if (matchPatternsWithOffset(ctx, queryPattern, policyPattern, offset)) return true
            if (matchPatternsWithOffset(ctx, queryPattern, inversePolicyPattern, offset)) return true
        }

        return false
    }

    private fun matchPatternsWithOffset(ctx: Context, queryPattern: AstNode, rulePattern: AstNode, offset: Int): Boolean {
        for (i in 0 until rulePattern.elements.size) {
            val queryElement = queryPattern.elements[i]
            val ruleElement = rulePattern.elements[i + offset]
            if (!matchPatternElement(queryElement, ruleElement)) {
                return false
            }
            if (ctx.ruleSubjectNode == ruleElement) {
                ctx.querySubjectNode = queryElement.asNode()
            }
        }
        return true
    }

    private fun matchPatternElement(queryElement: AstElement, policyElement: AstElement): Boolean {
        if (queryElement.javaClass != policyElement.javaClass) {
            return false
        }

        return when {
            areNodes(queryElement, policyElement) -> compareNodes(queryElement as AstNode, policyElement as AstNode)
            areRelations(queryElement, policyElement) -> compareRelation(
                queryElement as AstNode,
                policyElement as AstNode
            )

            else -> false
        }

    }

    private fun areNodes(a: AstElement, b: AstElement): Boolean {
        return a.type == AstType.NODE && b.type == AstType.NODE
    }

    private fun areRelations(a: AstElement, b: AstElement): Boolean {
        val relationTypes = setOf(AstType.RELATION_BOTH, AstType.RELATION_LEFT, AstType.RELATION_RIGHT)
        return a.type in relationTypes && b.type in relationTypes
    }

    private fun compareRelation(queryNode: AstNode, policyNode: AstNode): Boolean {
        if (queryNode.type != AstType.RELATION_BOTH && policyNode.type != AstType.RELATION_BOTH && policyNode.type != queryNode.type) {
            return false
        }

        val queryLabels = queryNode.elements.filter { it.type == AstType.RELATION_LABEL }.map { (it as AstValue).value }
        val policyLabels =
            policyNode.elements.filter { it.type == AstType.RELATION_LABEL }.map { (it as AstValue).value }

        for (label in policyLabels) {
            if (queryLabels.contains(label)) {
                return true
            }
        }

        return false
    }

    private fun compareNodes(queryNode: AstNode, policyNode: AstNode): Boolean {
        val queryLabels = queryNode.elements.filter { it.type == AstType.NODE_LABEL }.map { (it as AstValue).value }
        val policyLabels = policyNode.elements.filter { it.type == AstType.NODE_LABEL }.map { (it as AstValue).value }
        return queryLabels.containsAll(policyLabels)
    }


    private fun inversePattern(node: AstNode): AstNode {
        if (node.type != AstType.PATTERN) error("Only PATTERN nodes can be inversed")

        val inversedNode = AstNode(AstType.PATTERN)
        node.elements.asReversed().forEach {
            val newType = when (it.type) {
                AstType.RELATION_RIGHT -> AstType.RELATION_LEFT
                AstType.RELATION_LEFT -> AstType.RELATION_RIGHT
                else -> it.type
            }
            val copy = it.copy(newType)
            inversedNode.elements.add(copy)
        }
        return inversedNode
    }

    private class Context {
        var querySubjectNode: AstNode? = null
        var ruleSubjectNode: AstNode? = null
        var currentMatch: AstNode? = null
        var currentWhere: AstNode? = null


        val detections: MutableList<Detection> = mutableListOf()

    }

}
