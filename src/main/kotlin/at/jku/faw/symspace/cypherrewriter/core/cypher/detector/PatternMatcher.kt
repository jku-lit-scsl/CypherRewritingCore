package at.jku.faw.symspace.cypherrewriter.core.cypher.detector

import at.jku.faw.symspace.cypherrewriter.core.cypher.AstElement
import at.jku.faw.symspace.cypherrewriter.core.cypher.AstNode
import at.jku.faw.symspace.cypherrewriter.core.cypher.AstType
import at.jku.faw.symspace.cypherrewriter.core.cypher.parser.NewCypherParser
import org.springframework.stereotype.Component
import java.lang.IllegalArgumentException

@Component
class PatternMatcher(private val cypherParser: NewCypherParser) : SubDetector {

    private val policyAst = cypherParser.parse("MATCH (a)--(b)-[:TEST]->(c) RETURN *").asNode().elements.find { it.type == AstType.MATCH }!!


    fun matches(queryMatchNode: AstNode, policyMatchNode: AstNode) {

        val queryPaths = getPaths(queryMatchNode)
        val policyPaths = getPaths(policyMatchNode)

        queryPaths.forEach { println(it) }
        println()
        policyPaths.forEach { println(it) }
        println()
    }

    private fun getPaths(astNode: AstNode): Set<Path> {
        val patterns = astNode.elements.filter { it.type == AstType.PATTERN }
        return patterns.flatMap { evalPattern(it.asNode()) }.toSet()
    }

    private fun evalPattern(pattern: AstNode): Set<Path> {
        val paths = mutableSetOf<Path>()

        for (i in 0 until pattern.elements.size step 2) {
            if (i + 2 < pattern.elements.size) {
                val nodeA = getNode(pattern.elements[i])
                val astRelation = pattern.elements[i+1]
                val relation = getRelation(astRelation)
                val nodeB = getNode(pattern.elements[i+2])

                when (astRelation.type) {
                    AstType.RELATION_BOTH -> {
                        paths.add(Path(nodeA, relation, nodeB))
                        paths.add(Path(nodeB, relation, nodeA))
                    }
                    AstType.RELATION_RIGHT -> {
                        paths.add(Path(nodeA, relation, nodeB))
                    }
                    AstType.RELATION_LEFT -> {
                        paths.add(Path(nodeB, relation, nodeA))
                    }
                    else -> {
                        throw IllegalArgumentException("Invalid relation encountered")
                    }
                }

            }
        }

        return paths
    }

    private fun getVariable(ast: AstElement): String {
        return ast.asNode().elements.find { it.type == AstType.VARIABLE }?.asValue()?.value?.toString() ?: "*"
    }

    private fun getLabels(ast: AstElement): List<String> {
        return ast.asNode().elements.filter { it.type == AstType.NODE_LABEL || it.type == AstType.RELATION_LABEL }.map { it.asValue().value.toString() }
    }

    private fun getNode(ast: AstElement): Node {
        val variable = getVariable(ast)
        val labels = getLabels(ast)
        return Node(variable, labels)
    }

    private fun getRelation(ast: AstElement): Relation {
        val variable = getVariable(ast)
        val labels = getLabels(ast)
        return Relation(variable, labels)
    }

    private data class Path(val from: Node, val relationship: Relation, val to: Node) {
        override fun toString(): String {
            return "$from-$relationship->$to"
        }
    }

    private data class Node(val variable: String, val labels: List<String>) {
        override fun toString(): String {
            return "($variable:${labels.joinToString(":")})"
        }
    }
    private data class Relation(val variable: String, val labels: List<String>) {
        override fun toString(): String {
            return "[$variable:${labels.joinToString(":")}]"
        }
    }

    @Deprecated("only debugging code")
    override fun process(ast: AstElement): List<Detection> {
        traverse(ast)
        return listOf()
    }

    private fun traverse(ast: AstElement) {
        if (ast is AstNode) {
            if (ast.type == AstType.MATCH) {
                matches(ast, policyAst.asNode())
            } else {
                ast.elements.forEach { traverse(it) }
            }
        }
    }
}


