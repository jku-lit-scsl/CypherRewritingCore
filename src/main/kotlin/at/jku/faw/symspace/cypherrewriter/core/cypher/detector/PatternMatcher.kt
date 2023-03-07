package at.jku.faw.symspace.cypherrewriter.core.cypher.detector

import at.jku.faw.symspace.cypherrewriter.core.cypher.AstElement
import at.jku.faw.symspace.cypherrewriter.core.cypher.AstNode
import at.jku.faw.symspace.cypherrewriter.core.cypher.AstType
import at.jku.faw.symspace.cypherrewriter.core.cypher.parser.NewCypherParser
import org.springframework.stereotype.Component
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

@Component
class PatternMatcher(private val cypherParser: NewCypherParser) : SubDetector {

    private val policyAst = cypherParser.parse("MATCH (p1:Node1)-[:pTestRel]-(p2) RETURN *")
        .asNode().elements.find { it.type == AstType.MATCH }!!


    fun matches(queryMatchNode: AstNode, policyMatchNode: AstNode) {

        println("match test")
        val queryPaths = getPaths(queryMatchNode)
        val policyPaths = getPaths(policyMatchNode)
        val nodes = getNode()

        println("extracted paths")
        queryPaths.forEach { println(it) }
        println()
        policyPaths.forEach { println(it) }
        println()

        println("structural matches")
        val structuralMatches = findStructuralMatches(queryPaths, policyPaths)
        structuralMatches.forEach { println(it) }
        println()

        println("vars and labels")
        val policyVarLabels = pathsToVarLabelMap(policyPaths).also { println(it) }
        val queryVarLabels = pathsToVarLabelMap(queryPaths).also { println(it) }

        println("var mapping candidates")
        val possibleNodeVarMappings = findVarMappingCandidates(structuralMatches)
        println(possibleNodeVarMappings)

        val iter = VariableMappingIterator.of(possibleNodeVarMappings)
        for (mappings in iter) {
            println(mappings)

        }
    }

    private fun pathsToVarLabelMap(paths: Set<Path>): Map<String, Set<String>> {
        val map = mutableMapOf<String, MutableSet<String>>()
        for (path in paths) {
            val toKey = path.to.variable
            val toLabels = path.to.labels
            map.getOrPut(toKey) { mutableSetOf() }.addAll(toLabels)
            val fromKey = path.from.variable
            val fromLabels = path.from.labels
            map.getOrPut(fromKey) { mutableSetOf() }.addAll(fromLabels)
        }
        return map
    }

    private class VariableMappingIterator(val items: List<MappingItem>) : Iterator<Set<Mapping>> {

        var next = nextValue()
        private var increaseNext = false
        private var wasIllegalMapping = false
        override fun hasNext(): Boolean {
            return next != null
        }

        override fun next(): Set<Mapping> {
            val returnValue = next
            generateNewNext()
            if (returnValue != null) {
                return returnValue
            } else {
                throw IllegalStateException("Iterator is empty.")
            }
        }

        private fun generateNewNext() {
            var newNext: Set<Mapping>?
            do {
                wasIllegalMapping = false
                newNext = nextValue()
            } while (wasIllegalMapping)
            next = newNext
        }

        private fun nextValue(): Set<Mapping>? {
            val mappings = mutableSetOf<Mapping>()

            for (item in items) {
                if (increaseNext) {
                    if (item.isEmpty()) {
                        increaseNext = true
                        item.reset()
                    } else {
                        increaseNext = false
                        item.next()
                    }
                }

                val mapping = item.get()
                if (mappings.any { it.src != mapping.src && it.dest == mapping.dest }) {
                    wasIllegalMapping = true
                    increaseNext = true
                    return null
                }
                mappings.add(mapping)
            }

            if (increaseNext) {
                return null
            }
            increaseNext = true
            return mappings
        }

        companion object {
            fun of(possibleMappings: Map<String, Set<String>>): VariableMappingIterator {
                val items = mutableListOf<MappingItem>()
                for ((key, values) in possibleMappings) {
                    items.add(MappingItem(key, values))
                }
                return VariableMappingIterator(items)
            }
        }
    }

    private class MappingItem(val src: String, val dests: Iterable<String>) {
        private var iterator = dests.iterator()
        private var value = iterator.next()

        fun isEmpty(): Boolean {
            return !iterator.hasNext()
        }

        fun get(): Mapping {
            return Mapping(src, value)
        }

        fun next() {
            value = iterator.next()
        }

        fun reset() {
            iterator = dests.iterator()
            next()
        }
    }

    private data class Mapping(val src: String, val dest: String)

    private fun findVarMappingCandidates(structuralMatches: Set<StructuralMatch>): Map<String, Set<String>> {
        val possibleNodeVarMappings = mutableMapOf<String, MutableSet<String>>()
        structuralMatches.forEach {
            possibleNodeVarMappings.getOrPut(it.policyPath.from.variable) { mutableSetOf() }
                .add(it.queryPath.from.variable)
            possibleNodeVarMappings.getOrPut(it.policyPath.to.variable) { mutableSetOf() }.add(it.queryPath.to.variable)
        }
        return possibleNodeVarMappings
    }

    private fun findStructuralMatches(queryPaths: Set<Path>, policyPaths: Set<Path>): Set<StructuralMatch> {
        val result = mutableSetOf<StructuralMatch>()

        for (q in queryPaths) {
            for (p in policyPaths) {
                if (isStructuralMatch(q, p)) {
                    result.add(StructuralMatch(q, p))
                }
            }
        }

        return result
    }

    private fun isStructuralMatch(queryPath: Path, policyPath: Path): Boolean {
        val fromLabelsMatch =
            queryPath.from.labels.contains("*") || policyPath.from.labels.contains("*") || queryPath.from.labels.containsAll(
                policyPath.from.labels
            )
        val toLabelsMatch =
            queryPath.to.labels.contains("*") || policyPath.to.labels.contains("*") || queryPath.to.labels.containsAll(
                policyPath.to.labels
            )
        val relationLabelsMatch =
            queryPath.relationship.labels.contains("*") || policyPath.relationship.labels.contains("*") || queryPath.relationship.labels.intersect(
                policyPath.relationship.labels
            ).isNotEmpty()

        return fromLabelsMatch && toLabelsMatch && relationLabelsMatch
    }

    private data class StructuralMatch(val queryPath: Path, val policyPath: Path)

    private fun getPaths(astNode: AstNode): Set<Path> {
        val patterns = astNode.elements.filter { it.type == AstType.PATTERN }
        return patterns.flatMap { evalPattern(it.asNode()) }.toSet()
    }

    private fun evalPattern(pattern: AstNode): Set<Path> {
        val paths = mutableSetOf<Path>()

        for (i in 0 until pattern.elements.size step 2) {
            if (i + 2 < pattern.elements.size) {
                val nodeA = getNode(pattern.elements[i])
                val astRelation = pattern.elements[i + 1]
                val relation = getRelation(astRelation)
                val nodeB = getNode(pattern.elements[i + 2])

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

    private fun getLabels(ast: AstElement): Set<String> {
        return ast.asNode().elements.filter { it.type == AstType.NODE_LABEL || it.type == AstType.RELATION_LABEL }
            .map { it.asValue().value.toString() }.toSet().ifEmpty { setOf("*") }
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

    private data class Node(val variable: String, val labels: Set<String>) {
        override fun toString(): String {
            return "($variable:${labels.joinToString(":")})"
        }
    }

    private data class Relation(val variable: String, val labels: Set<String>) {
        override fun toString(): String {
            return "[$variable:${labels.joinToString(":")}]"
        }
    }

    @Deprecated("only debugging code")
    override fun process(ast: AstElement): List<Detection> {
        val ctx = Context()
        traverse(ctx, ast)
        return listOf()
    }

    private fun traverse(ctx: Context, ast: AstElement) {
        if (ast is AstNode) {
            if (ast.type == AstType.MATCH) {
                //matches(ast, policyAst.asNode())
            } else if (ast.type == AstType.PATTERN) {
                processPattern(ctx, ast)
            } else {
                ast.elements.forEach { traverse(ctx, it) }
            }
        }
    }

    private fun processPattern(ctx: Context, ast: AstNode) {
        evalPattern(ast)
    }

    private data class Context(val policy: PolicyContext = PolicyContext(), val query: QueryContext = QueryContext())
    private data class PolicyContext(val paths: Set<Path> = mutableSetOf(), val nodes: Set<Node> = mutableSetOf())
    private data class QueryContext(val paths: Set<Path> = mutableSetOf(), val nodes: Set<Node> = mutableSetOf())
}


