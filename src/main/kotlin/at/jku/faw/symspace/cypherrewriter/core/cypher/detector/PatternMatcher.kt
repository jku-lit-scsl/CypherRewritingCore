package at.jku.faw.symspace.cypherrewriter.core.cypher.detector

import at.jku.faw.symspace.cypherrewriter.core.cypher.*
import at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils.*
import at.jku.faw.symspace.cypherrewriter.core.cypher.parser.NewCypherParser
import org.springframework.stereotype.Component
import kotlin.IllegalArgumentException
import kotlin.IllegalStateException

@Component
class PatternMatcher(
    private val cypherParser: NewCypherParser,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection") private val permissionConfig: PermissionConfig
) : PermissionDetector {

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
        ctx.policy.variables.forEach { println("\t\t" + it.name + ": " + it.knownLabels) }
        println("\tquery")
        ctx.query.variables.forEach { println("\t\t" + it.name + ": " + it.knownLabels) }
        println()

        findVarMappingCandidates(ctx)
        println("var mapping candidates: " + ctx.possibleVarMappings)

        val mappings = findMappings(ctx)
        println("valid var mappings")
        println(mappings)

        println("var states")
        ctx.query.variables.forEach { println("${it.name} = ${it.state}") }

        return if (mappings.size == 1) {
            ctx.validVarMappings.addAll(mappings.first())
            true
        } else {
            false
        }
    }

    private fun findMappings(ctx: Context): Set<Set<Mapping>> {
        val resultMappings = mutableSetOf<Set<Mapping>>()
        val iter = VariableMappingIterator.of(ctx.possibleVarMappings)

        for (mappings in iter) {
            println(mappings)
            var mappingValid = true
            for (mapping in mappings) {
                val policyLabels = mapping.policyVar.knownLabels
                val queryLabels = mapping.queryVar.knownLabels

                val mapper = ctx.policy.labelMatchers[mapping.policyVar.name]
                val result = if (mapper != null) {
                    mapper(queryLabels, policyLabels)
                } else {
                    matchLabelsQueryContainsAllPolicy(queryLabels, policyLabels, true)
                }
                println("$policyLabels containsAll $queryLabels: $result")
                mappingValid = mappingValid && result
            }
            if (mappingValid) {
                resultMappings.add(mappings)
            }
            println("Mapping is valid: $mappingValid")
            println()
        }

        return resultMappings
    }

    private fun findVarMappingCandidates(ctx: Context) {
        ctx.structuralMatches.forEach {
            ctx.possibleVarMappings.getOrPut(it.policyPath.from.variable) { mutableSetOf() }
                .add(it.queryPath.from.variable)

            if (it.policyPath.relationship != null && it.queryPath.relationship != null) {
                ctx.possibleVarMappings.getOrPut(it.policyPath.relationship.variable) { mutableSetOf() }
                    .add(it.queryPath.relationship.variable)
            }

            if (it.policyPath.to != null && it.queryPath.to != null) {
                ctx.possibleVarMappings.getOrPut(it.policyPath.to.variable) { mutableSetOf() }
                    .add(it.queryPath.to.variable)
            }
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
        nodeLabelsMatcher: Map<String, (Set<String>, Set<String>) -> Boolean>, queryPath: Path, policyPath: Path
    ): Boolean {
        val fromLabelsMatch = nodeLabelsMatch(nodeLabelsMatcher, queryPath.from, policyPath.from)
        val toLabelsMatch = nodeLabelsMatch(nodeLabelsMatcher, queryPath.to, policyPath.to)
        val relationLabelsMatch = relationLabelsMatch(queryPath.relationship, policyPath.relationship)

        return fromLabelsMatch && toLabelsMatch && relationLabelsMatch
    }

    private fun nodeLabelsMatch(
        nodeLabelsMatcher: Map<String, (Set<String>, Set<String>) -> Boolean>, queryNode: Node?, policyNode: Node?
    ): Boolean {
        if (queryNode == null && policyNode == null) {
            return true
        }
        if (queryNode == null || policyNode == null) {
            return false
        }

        val matcher = nodeLabelsMatcher[policyNode.variable.name]
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

    @Deprecated("rebuild")
    private fun evalSingleNodePattern(ctx: CommonContext, pattern: AstNode): Set<Path> {
        val cypherNode = pattern.elements.find { it.type == AstType.NODE }
        if (cypherNode != null) {
            val path = Path(getNode(ctx, cypherNode), null, null)
            return setOf(path)
        }
        throw IllegalStateException("This method was called on a pattern it was not designed for. Probably an issue with isOnlyOneNode().")
    }

    private fun getVariable(ctx: CommonContext, ast: AstNode): Variable {
        val name = ast.elements.find { it.type == AstType.VARIABLE }?.asValue()?.value?.toString() ?: "*"
        return createNewVariable(ctx, name)
    }

    private fun getLabels(ast: AstElement): Set<String> {
        return ast.asNode().elements.filter { it.type == AstType.NODE_LABEL || it.type == AstType.RELATION_LABEL }
            .map { it.asValue().value.toString() }.toSet().ifEmpty { setOf("*") }
    }

    private fun getNode(ctx: CommonContext, ast: AstElement): Node {
        val parentNodeBackup = ctx.parentNode
        ctx.parentNode = ast.asNode()
        val variable = getVariable(ctx, ast.asNode())
        ctx.parentNode = parentNodeBackup

        val labels = getLabels(ast)
        variable.knownLabels.addAll(labels)
        return Node(variable, labels)
    }

    private fun getRelation(ctx: CommonContext, ast: AstElement): Relation {
        val parentNodeBackup = ctx.parentNode
        ctx.parentNode = ast.asNode()
        val variable = getVariable(ctx, ast.asNode())
        ctx.parentNode = parentNodeBackup

        val labels = getLabels(ast)
        variable.knownLabels.addAll(labels)
        return Relation(variable, ast.type, labels)
    }

    override fun process(node: AstNode): List<Detection> {
        val detections = mutableListOf<Detection>()

        val queryContext = QueryContext()
        traverse(queryContext, node)

        for (policy in permissionConfig.policies) {
            val policyContext = policyContextCache.getOrPut(policy) { generatePolicyContext(policy) }
            val ctx = Context(policy = policyContext, query = queryContext)
            val matches = matches(ctx)
            println("policy matches: $matches")
            if (matches) {
                for (rule in policy.rules) {
                    if (ruleMatches(rule, ctx)) {
                        val detection = Detection(rule)
                        detection.enforcementNode = ctx.query.matchClause
                        detection.protectedNode =
                            translatePolicyVarToQueryVar(rule.variable, ctx.validVarMappings).definingElement
                        detections.add(detection)
                    }
                }
            }
        }

        return detections
    }

    private fun ruleMatches(rule: Rule, ctx: Context): Boolean {
        return rule.conditions.all {
            val queryVar = translatePolicyVarToQueryVar(it.variable, ctx.validVarMappings)
            statesMatch(queryVar.state, it.filterType, it.returnType)
        }
    }

    private fun statesMatch(
        queryVarState: VariableState, policyFilterType: FilterType, policyReturnType: ReturnType
    ): Boolean {
        val filterTypeResult =
            policyFilterType == FilterType.ANY
                    || queryVarState.filterType == FilterType.ANY
                    || queryVarState.filterType == policyFilterType
        val returnTypeResult =
            policyReturnType == ReturnType.ANY
                    || queryVarState.returnType == ReturnType.ANY
                    || queryVarState.returnType == policyReturnType
                    || policyReturnType == ReturnType.ANY_RETURN && queryVarState.returnType in ReturnTypeMetadata.anyReturnTypes

        return filterTypeResult && returnTypeResult
    }

    private fun translatePolicyVarToQueryVar(policyVarName: String, mappings: Set<Mapping>): Variable {
        return mappings.find { it.policyVar.name == policyVarName }?.queryVar
            ?: throw IllegalStateException("Tried to translate unmapped policy variable.")
    }

    private fun generatePolicyContext(policy: Policy): PolicyContext {
        val antlrPatternContext = cypherParser.getParser(policy.pattern).oC_Pattern()
        val ast = cypherParser.parse(antlrPatternContext)

        val labelMatchers = policy.patternMatchStrategy.map {
            when (it.value.labelStrategy) {
                LabelMatchStrategy.CONTAINS_ANY -> it.key to { policyLabels: Set<String>, queryLabels: Set<String> ->
                    matchLabelsQueryContainsAnyPolicy(
                        policyLabels, queryLabels, it.value.matchEmptyLabels
                    )
                }

                LabelMatchStrategy.CONTAINS_ALL -> it.key to { policyLabels: Set<String>, queryLabels: Set<String> ->
                    matchLabelsQueryContainsAllPolicy(
                        policyLabels, queryLabels, it.value.matchEmptyLabels
                    )
                }

                LabelMatchStrategy.EXACT -> it.key to { policyLabels: Set<String>, queryLabels: Set<String> ->
                    matchLabelsExactly(
                        policyLabels, queryLabels, it.value.matchEmptyLabels
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
            AstType.MATCH -> processMatch(ctx, ast.asNode())
            else -> traverseChildren(ctx, ast)
        }
    }

    private fun processMatch(ctx: CommonContext, ast: AstNode) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            ctx.matchClause = ast
        }
        traverseChildren(ctx, ast)
    }

    private fun processFunctionName(ctx: CommonContext, ast: AstValue) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            ctx.isAggregationFunction = ast.value.toString() in ReturnTypeMetadata.aggregationFunctions
        }
    }

    private fun processAsterisk(ctx: CommonContext) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            ctx.variables.forEach { mergeReturnType(it.state, ReturnType.RETURNED_AS_VALUE) }
        }
    }

    private fun traverseChildren(ctx: CommonContext, ast: AstElement) {
        if (ast is AstNode) {
            ast.elements.forEach {
                ctx.parentNode = ast
                traverse(ctx, it)
            }
        }
    }

    private fun processReturn(ctx: CommonContext, ast: AstNode) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            ctx.isReturnClause = true
            traverseChildren(ctx, ast)
            ctx.isReturnClause = false
        } else {
            traverseChildren(ctx, ast)
        }
    }

    private fun processPattern(ctx: CommonContext, ast: AstNode) {
        ctx.patternContext = PatternContext()
        traverseChildren(ctx, ast)
        ctx.patternContext?.let { ctx.paths.addAll(it.getPaths()) }
        ctx.patternContext = null
    }

    private fun getFilterType(ctx: QueryContext): FilterType {
        return if (ctx.isFiltered) {
            FilterType.FILTERED
        } else {
            FilterType.NO_FILTER
        }
    }

    private fun getReturnType(ctx: QueryContext): ReturnType {
        return if (ctx.isAggregationFunction) {
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
        val node = getNode(ctx, ast)
        ctx.nodes.add(node)
        ctx.patternContext?.add(node)

        traverseChildren(ctx, ast)

        updateFilterStatusOfLastVar(ctx)
    }

    private fun processRelation(ctx: CommonContext, ast: AstNode) {
        val relation = getRelation(ctx, ast)
        ctx.relations.add(relation)
        ctx.patternContext?.add(relation)

        traverseChildren(ctx, ast)

        updateFilterStatusOfLastVar(ctx)
    }

    private fun processWhere(ctx: CommonContext, ast: AstNode) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            ctx.isWhereClause = true
        }
        traverseChildren(ctx, ast)
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            ctx.isWhereClause = false
        }
    }


    private fun updateFilterStatusOfLastVar(ctx: CommonContext) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            val lastVar = ctx.lastVar
            if (lastVar != null) {
                val varState = lastVar.state
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
                val varState = lastVar.state
                mergeReturnType(varState, getReturnType(ctx))
            }
            ctx.lastVar = null
            ctx.isAggregationFunction = false
        }
    }

    private fun processVariable(ctx: CommonContext, ast: AstValue) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            val variableName = ast.value.toString()
            val variable = ctx.variables.find { it.name == variableName }
                ?: throw IllegalArgumentException("The AST appears to be malformed: A variable was not already created by the parent-node processing.")
            ctx.lastVar = variable
        }
    }

    private fun createNewVariable(ctx: CommonContext, variableName: String): Variable {
        val parentNode = ctx.parentNode
        val variable = if (parentNode != null && parentNode.type in AstMetadata.SET_OF_VARIABLE_STORABLE_AST_TYPES) {
            Variable(variableName, parentNode)
        } else {
            throw IllegalArgumentException("The AST appears to be malformed: Try to define a variable in an AstNode not suited.")
        }
        ctx.variables.add(variable)

        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            ctx.lastVar = variable
        }
        return variable
    }

    private fun processPropertyDotAccess(ctx: CommonContext, ast: AstNode) {
        traverseChildren(ctx, ast)
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            if (ctx.isWhereClause) {
                ctx.isFiltered = true
                updateFilterStatusOfLastVar(ctx)
            } else if (ctx.isReturnClause) {
                updateReturnStatusOfLastVar(ctx)
            }


        }
    }

    private fun anyAsteriskLabels(queryLabels: Set<String>, policyLabels: Set<String>): Boolean {
        return queryLabels.contains("*") || policyLabels.contains("*")
    }

    private fun matchLabelsExactly(
        queryLabels: Set<String>, policyLabels: Set<String>, matchEmptyQueryLabels: Boolean = false
    ): Boolean {
        return anyAsteriskLabels(
            queryLabels, policyLabels
        ) || queryLabels == policyLabels || matchEmptyQueryLabels && queryLabels.isEmpty()
    }

    private fun matchLabelsQueryContainsAllPolicy(
        queryLabels: Set<String>, policyLabels: Set<String>, matchEmptyQueryLabels: Boolean = false
    ): Boolean {
        return anyAsteriskLabels(
            queryLabels, policyLabels
        ) || queryLabels.containsAll(policyLabels) || matchEmptyQueryLabels && queryLabels.isEmpty()
    }

    private fun matchLabelsQueryContainsAnyPolicy(
        queryLabels: Set<String>, policyLabels: Set<String>, matchEmptyQueryLabels: Boolean = false
    ): Boolean {
        return anyAsteriskLabels(
            queryLabels, policyLabels
        ) || policyLabels.any { queryLabels.contains(it) } || matchEmptyQueryLabels && queryLabels.isEmpty()
    }

}


