package at.jku.faw.symspace.cypherrewriter.core.cypher.detector

import at.jku.faw.symspace.cypherrewriter.core.cypher.*
import at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils.*
import at.jku.faw.symspace.cypherrewriter.core.cypher.parser.NewCypherParser
import org.springframework.stereotype.Component
import java.lang.IllegalStateException
import kotlin.IllegalArgumentException

@Component
class PatternMatcher(private val cypherParser: NewCypherParser, private val permissionConfig: PermissionConfig) :
    SubDetector {

    private val policyContextCache: MutableMap<Policy, PolicyContext> = mutableMapOf()

    private fun matches(ctx: Context): Boolean {

        println("match test")

        println("extracted paths")
        ctx.query.paths.forEach { println(it) }
        println()
        ctx.policy.paths.forEach { println(it) }
        println()

        println("structural matches")
        findStructuralMatches(ctx)
        ctx.structuralMatches.forEach { println(it) }
        println()

        println("vars and labels")
        println("\tpolicy")
        ctx.policy.varToLabels.forEach { println("\t\t" + it) }
        println("\tquery")
        ctx.query.varToLabels.forEach { println("\t\t" + it) }
        println()

        findVarMappingCandidates(ctx)
        println("var mapping candidates: " + ctx.possibleVarMappings)

        findMappings(ctx)
        println("valid var mappings")
        println(ctx.validVarMappings)

        println("var states")
        ctx.query.varStates.forEach{println(it)}

        return ctx.validVarMappings.size == 1
    }

    private fun findMappings(ctx: Context) {
        val iter = VariableMappingIterator.of(ctx.possibleVarMappings)
        for (mappings in iter) {
            println(mappings)
            var mappingValid = true
            for (mapping in mappings) {
                val policyLabels = ctx.policy.varToLabels[mapping.policyVar] ?: setOf()
                val queryLabels = ctx.query.varToLabels[mapping.queryVar] ?: setOf()

                val mapper = ctx.policy.labelMatchers[mapping.policyVar]
                val result = if (mapper != null) {
                    mapper(queryLabels, policyLabels)
                } else {
                    matchLabelsQueryContainsAllPolicy(queryLabels, policyLabels, true)
                }
                println("$policyLabels containsAll $queryLabels: $result")
                mappingValid = mappingValid && result
            }
            if (mappingValid) {
                ctx.validVarMappings.add(mappings)
            }
            println("Mapping is valid: $mappingValid")
            println()
        }
    }

    private fun findVarMappingCandidates(ctx: Context) {
        ctx.structuralMatches.forEach {
            ctx.possibleVarMappings.getOrPut(it.policyPath.from.variable) { mutableSetOf() }
                .add(it.queryPath.from.variable)

            if (it.policyPath.to != null && it.queryPath.to != null)
                ctx.possibleVarMappings.getOrPut(it.policyPath.to.variable) { mutableSetOf() }
                    .add(it.queryPath.to.variable)
        }
    }

    private fun findStructuralMatches(ctx: Context) {
        for (q in ctx.query.paths) {
            for (p in ctx.policy.paths) {
                if (isStructuralMatch(ctx.policy.labelMatchers, q, p)) {
                    ctx.structuralMatches.add(StructuralMatch(q, p))
                }
            }
        }
    }

    private fun isStructuralMatch(
        nodeLabelsMatcher: Map<String, (Set<String>, Set<String>) -> Boolean>,
        queryPath: Path,
        policyPath: Path
    ): Boolean {
        val fromLabelsMatch = nodeLabelsMatch(nodeLabelsMatcher, queryPath.from, policyPath.from)
        val toLabelsMatch = nodeLabelsMatch(nodeLabelsMatcher, queryPath.to, policyPath.to)
        val relationLabelsMatch = relationLabelsMatch(queryPath.relationship, policyPath.relationship)

        return fromLabelsMatch && toLabelsMatch && relationLabelsMatch
    }

    private fun nodeLabelsMatch(
        nodeLabelsMatcher: Map<String, (Set<String>, Set<String>) -> Boolean>,
        queryNode: Node?,
        policyNode: Node?
    ): Boolean {
        if (queryNode == null && policyNode == null) {
            return true
        }
        if (queryNode == null || policyNode == null) {
            return false
        }

        val matcher = nodeLabelsMatcher[policyNode.variable]
        if (matcher != null) {
            return matcher(queryNode.labels, policyNode.labels)
        }

        return matchLabelsQueryContainsAllPolicy(queryNode.labels, policyNode.labels, true)
    }

    private fun relationLabelsMatch(queryRelation: Relation?, policyRelation: Relation?): Boolean {
        if (queryRelation == null && policyRelation == null) {
            return true
        }
        if (queryRelation == null || policyRelation == null) {
            return false
        }

        return queryRelation.labels.contains("*") || policyRelation.labels.contains("*") || queryRelation.labels.intersect(
            policyRelation.labels
        ).isNotEmpty()
    }

    private fun evalPattern(pattern: AstNode): Set<Path> {
        return if (isOnlyOneNode(pattern)) {
            evalSingleNodePattern(pattern)
        } else {
            evalGeneralPattern(pattern)
        }
    }

    private fun evalGeneralPattern(pattern: AstNode): Set<Path> {
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

    private fun evalSingleNodePattern(pattern: AstNode): Set<Path> {
        val cypherNode = pattern.elements.find { it.type == AstType.NODE }
        if (cypherNode != null) {
            val path = Path(getNode(cypherNode), null, null)
            return setOf(path)
        }
        throw IllegalStateException("This method was called on a pattern it was not designed for. Probably an issue with isOnlyOneNode().")
    }

    private fun isOnlyOneNode(pattern: AstNode): Boolean {
        return pattern.elements.count { it.type == AstType.NODE } == 1
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

    @Deprecated("only debugging code")
    override fun process(ast: AstElement): List<Detection> {
        val detections = mutableListOf<Detection>()

        val queryContext = QueryContext()
        traverse(queryContext, ast)

        for (policy in permissionConfig.policies) {
            val policyContext = policyContextCache.getOrPut(policy) { generatePolicyContext(policy) }
            val ctx = Context(policy = policyContext, query = queryContext)
            val matches = matches(ctx)
            println("policy matches: $matches")
        }

        return listOf()
    }

    private fun generatePolicyContext(policy: Policy): PolicyContext {
        val antlrPatternContext = cypherParser.getParser(policy.pattern).oC_Pattern()
        val ast = cypherParser.parse(antlrPatternContext)

        val labelMatchers = policy.patternMatchStrategy.map {
            when (it.value.labelStrategy) {
                LabelMatchStrategy.CONTAINS_ANY -> it.key to { policyLabels: Set<String>, queryLabels: Set<String> ->
                    matchLabelsQueryContainsAnyPolicy(
                        policyLabels,
                        queryLabels,
                        it.value.matchEmptyLabels
                    )
                }

                LabelMatchStrategy.CONTAINS_ALL -> it.key to { policyLabels: Set<String>, queryLabels: Set<String> ->
                    matchLabelsQueryContainsAllPolicy(
                        policyLabels,
                        queryLabels,
                        it.value.matchEmptyLabels
                    )
                }

                LabelMatchStrategy.EXACT -> it.key to { policyLabels: Set<String>, queryLabels: Set<String> ->
                    matchLabelsExactly(
                        policyLabels,
                        queryLabels,
                        it.value.matchEmptyLabels
                    )
                }
            }
        }.toMap().toMutableMap()

        val ctx = PolicyContext(labelMatchers)
        traverse(ctx, ast)

        return ctx
    }

    private fun traverse(ctx: CommonContext, ast: AstElement) {
        when (ast.type) {
            AstType.PATTERN -> processPattern(ctx, ast.asNode())
            AstType.NODE -> processNode(ctx, ast.asNode())
            AstType.RELATION_LEFT -> processRelation(ctx, ast.asNode())
            AstType.RELATION_RIGHT -> processRelation(ctx, ast.asNode())
            AstType.RELATION_BOTH -> processRelation(ctx, ast.asNode())
            AstType.RETURN -> processReturn(ctx, ast.asNode())
            AstType.PROPERTIES -> processProperties(ctx, ast.asNode())
            AstType.VARIABLE -> processVariable(ctx, ast.asValue())
            AstType.WHERE -> processWhere(ctx, ast.asNode())
            AstType.PROPERTY_DOT_ACCESS -> processPropertyDotAccess(ctx, ast.asNode())
            AstType.ASTERISK -> processAsterisk(ctx)
            AstType.FUNCTION_NAME -> processFunctionName(ctx, ast.asValue())
            else -> traverseChildren(ctx, ast)
        }
    }

    private fun processFunctionName(ctx: CommonContext, ast: AstValue) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            ctx.isAggregated = ast.value.toString() in AggregationFunctions.names
        }
    }

    private fun processAsterisk(ctx: CommonContext) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            ctx.varStates.forEach { mergeReturnType(it.value, ReturnType.RETURNED_AS_VALUE) }
        }
    }

    private fun traverseChildren(ctx: CommonContext, ast: AstElement) {
        if (ast is AstNode) {
            ast.elements.forEach { traverse(ctx, it) }
        }
    }

    private fun processReturn(ctx: CommonContext, ast: AstNode) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            ctx.isReturn = true
            traverseChildren(ctx, ast)
            ctx.isReturn = false
        } else {
            traverseChildren(ctx, ast)
        }
    }

    private fun processPattern(ctx: CommonContext, ast: AstNode) {
        val paths = evalPattern(ast)
        ctx.paths.addAll(paths)

        traverseChildren(ctx, ast)
    }

    private fun getFilterType(ctx: QueryContext): FilterType {
        return if (ctx.isFiltered) {
            FilterType.FILTERED
        } else {
            FilterType.NO_FILTER
        }
    }

    private fun getReturnType(ctx: QueryContext): ReturnType {
        return if (ctx.isAggregated) {
            ReturnType.AGGREGATED
        } else {
            ReturnType.RETURNED_AS_VALUE
        }
    }

    private fun mergeFilterType(variableState: VariableState, filterType: FilterType) {
        if (filterType == FilterType.ANY) {
            throw IllegalArgumentException("$filterType must not be used here.")
        }

        if (variableState.filterType < filterType) {
            variableState.filterType = filterType
        }
    }

    private fun mergeReturnType(variableState: VariableState, returnType: ReturnType) {
        if (returnType == ReturnType.ANY) {
            throw IllegalArgumentException("$returnType must not be used here.")
        }

        if (variableState.returnType < returnType) {
            variableState.returnType = returnType
        }
    }

    private fun processProperties(ctx: CommonContext, ast: AstNode) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            ctx.isFiltered = true
        }
        traverseChildren(ctx, ast)
    }

    private fun processNode(ctx: CommonContext, ast: AstNode) {
        val node = getNode(ast)
        ctx.nodes.add(node)
        addLabelsToVarMap(ctx, node.variable, node.labels)

        traverseChildren(ctx, ast)

        updateFilterStatusOfLastVar(ctx)
    }

    private fun processRelation(ctx: CommonContext, ast: AstNode) {
        val relation = getRelation(ast)
        addLabelsToVarMap(ctx, relation.variable, relation.labels)


        traverseChildren(ctx, ast)

        updateFilterStatusOfLastVar(ctx)
    }

    private fun processWhere(ctx: CommonContext, ast: AstNode) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            ctx.isWhere = true
        }
        traverseChildren(ctx, ast)
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            ctx.isWhere = false
        }
    }



    private fun addLabelsToVarMap(ctx: CommonContext, variable: String, labels: Collection<String>) {
        if (variable != "*") {
            ctx.varToLabels.getOrPut(variable) { mutableSetOf() }
                .addAll(labels.filter { it != "*" })
        }
    }

    private fun updateFilterStatusOfLastVar(ctx: CommonContext) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            val lastVar = ctx.lastVar
            if (lastVar != null) {
                val varState = ctx.varStates.getOrPut(lastVar) { VariableState() }
                mergeFilterType(varState, getFilterType(ctx))
            }
            ctx.lastVar = null
            ctx.isFiltered = false
        }
    }

    private fun updateReturnStatusOfLastVar(ctx: CommonContext) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            val lastVar = ctx.lastVar
            if (lastVar != null) {
                val varState = ctx.varStates.getOrPut(lastVar) { VariableState() }
                mergeReturnType(varState, getReturnType(ctx))
            }
            ctx.lastVar = null
            ctx.isAggregated = false
        }
    }

    private fun processVariable(ctx: CommonContext, ast: AstValue) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            val variable = ast.value.toString()
            ctx.lastVar = variable
            ctx.varStates.computeIfAbsent(variable) { VariableState() }
        }
    }

    private fun processPropertyDotAccess(ctx: CommonContext, ast: AstNode) {
        traverseChildren(ctx, ast)
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            if (ctx.isWhere) {
                ctx.isFiltered = true
                updateFilterStatusOfLastVar(ctx)
            } else if (ctx.isReturn) {
                updateReturnStatusOfLastVar(ctx)
            }


        }
    }

    private fun anyAsteriskLabels(queryLabels: Set<String>, policyLabels: Set<String>): Boolean {
        return queryLabels.contains("*") || policyLabels.contains("*")
    }

    private fun matchLabelsExactly(
        queryLabels: Set<String>,
        policyLabels: Set<String>,
        matchEmptyQueryLabels: Boolean = false
    ): Boolean {
        return anyAsteriskLabels(
            queryLabels,
            policyLabels
        ) || queryLabels == policyLabels || matchEmptyQueryLabels && queryLabels.isEmpty()
    }

    private fun matchLabelsQueryContainsAllPolicy(
        queryLabels: Set<String>,
        policyLabels: Set<String>,
        matchEmptyQueryLabels: Boolean = false
    ): Boolean {
        return anyAsteriskLabels(
            queryLabels,
            policyLabels
        ) || queryLabels.containsAll(policyLabels) || matchEmptyQueryLabels && queryLabels.isEmpty()
    }

    private fun matchLabelsQueryContainsAnyPolicy(
        queryLabels: Set<String>,
        policyLabels: Set<String>,
        matchEmptyQueryLabels: Boolean = false
    ): Boolean {
        return anyAsteriskLabels(
            queryLabels,
            policyLabels
        ) || policyLabels.any { queryLabels.contains(it) } || matchEmptyQueryLabels && queryLabels.isEmpty()
    }

}


