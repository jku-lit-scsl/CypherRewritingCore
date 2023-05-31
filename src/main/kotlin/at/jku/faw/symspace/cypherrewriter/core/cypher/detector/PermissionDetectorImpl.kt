package at.jku.faw.symspace.cypherrewriter.core.cypher.detector

import at.jku.faw.symspace.cypherrewriter.core.cypher.*
import at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils.*
import at.jku.faw.symspace.cypherrewriter.core.cypher.parser.NewCypherParser
import org.springframework.stereotype.Component
import kotlin.IllegalArgumentException
import kotlin.IllegalStateException

@Component
class PermissionDetectorImpl(
    private val cypherParser: NewCypherParser,
    private val returnTypeHelper: ReturnTypeHelper,
    private val filterTypeHelper: FilterTypeHelper,
    private val labelMatcher: LabelMatcher,
    private val ruleHelper: RuleHelper,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection") private val permissionConfig: PermissionConfig
) : PermissionDetector {

    private val policyContextCache: MutableMap<Policy, PolicyContext> = mutableMapOf()

    private fun matches(ctx: Context): Boolean {
        findStructuralMatches(ctx)
        findVarMappingCandidates(ctx)
        val mappings = findMappings(ctx)

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
            var mappingValid = true
            for (mapping in mappings) {
                val policyLabels = mapping.policyVar.knownLabels
                val queryLabels = mapping.queryVar.knownLabels

                val mapper = ctx.policy.labelMatchers[mapping.policyVar.name]
                val result = if (mapper != null) {
                    mapper(queryLabels, policyLabels)
                } else {
                    labelMatcher.matchLabelsQueryContainsAllPolicy(queryLabels, policyLabels, true)
                }
                mappingValid = mappingValid && result
            }
            if (mappingValid) {
                resultMappings.add(mappings)
            }
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

        return labelMatcher.matchLabelsQueryContainsAllPolicy(queryNode.labels, policyNode.labels, true)
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

    private fun getVariable(ctx: CommonContext, ast: AstInternalNode): Variable {
        val name = ast.elements.find { it.type == AstType.VARIABLE }?.asValue()?.value?.toString() ?: "*"
        return createNewVariable(ctx, name)
    }

    private fun getLabels(ast: AstNode): Set<String> {
        return ast.asNode().elements.filter { it.type == AstType.NODE_LABEL || it.type == AstType.RELATION_LABEL }
            .map { it.asValue().value.toString() }.toSet().ifEmpty { setOf("*") }
    }

    private fun getNode(ctx: CommonContext, ast: AstNode): Node {
        val parentNodeBackup = ctx.parentNode
        ctx.parentNode = ast.asNode()
        val variable = getVariable(ctx, ast.asNode())
        ctx.parentNode = parentNodeBackup

        val labels = getLabels(ast)
        variable.knownLabels.addAll(labels)
        return Node(variable, labels)
    }

    private fun getRelation(ctx: CommonContext, ast: AstNode): Relation {
        val parentNodeBackup = ctx.parentNode
        ctx.parentNode = ast.asNode()
        val variable = getVariable(ctx, ast.asNode())
        ctx.parentNode = parentNodeBackup

        val labels = getLabels(ast)
        variable.knownLabels.addAll(labels)
        return Relation(variable, ast.type, labels)
    }

    override fun process(node: AstInternalNode): List<Detection> {
        val detections = mutableListOf<Detection>()

        val queryContext = QueryContext()
        traverse(queryContext, node)

        for (policy in permissionConfig.policies) {
            val policyContext = policyContextCache.getOrPut(policy) { generatePolicyContext(policy) }
            val ctx = Context(policy = policyContext, query = queryContext)
            val matches = matches(ctx)
            if (matches) {
                val varStateMap = ctx.validVarMappings.associate { it.policyVar.name to it.queryVar.state }
                for (rule in policy.rules) {
                    if (ruleHelper.ruleMatches(rule, varStateMap)) {
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

    private fun translatePolicyVarToQueryVar(policyVarName: String, mappings: Set<Mapping>): Variable {
        return mappings.find { it.policyVar.name == policyVarName }?.queryVar
            ?: throw IllegalStateException("Tried to translate unmapped policy variable.")
    }

    private fun generatePolicyContext(policy: Policy): PolicyContext {
        val antlrPatternContext = cypherParser.getParser(policy.pattern).oC_Pattern()
        val ast = cypherParser.parse(antlrPatternContext)

        val labelMatchers = labelMatcher.buildLabelMatchMap(policy.patternMatchStrategy)

        val ctx = PolicyContext(labelMatchers)
        traverse(ctx, ast)

        return ctx
    }

    private fun traverse(ctx: CommonContext, ast: AstNode) {
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

    private fun processMatch(ctx: CommonContext, ast: AstInternalNode) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            ctx.matchClause = ast
        }
        traverseChildren(ctx, ast)
    }

    private fun processFunctionName(ctx: CommonContext, ast: AstLeafValue) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            ctx.isAggregationFunction = ast.value.toString() in ReturnTypeMetadata.aggregationFunctions
        }
    }

    private fun processAsterisk(ctx: CommonContext) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            ctx.variables.forEach {
                it.state.returnType = returnTypeHelper.mergeReturnTypes(it.state.returnType, ReturnType.RETURNED_AS_VALUE)
            }
        }
    }

    private fun traverseChildren(ctx: CommonContext, ast: AstNode) {
        if (ast is AstInternalNode) {
            ast.elements.forEach {
                ctx.parentNode = ast
                traverse(ctx, it)
            }
        }
    }

    private fun processReturn(ctx: CommonContext, ast: AstInternalNode) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            ctx.isReturnClause = true
            traverseChildren(ctx, ast)
            ctx.isReturnClause = false
        } else {
            traverseChildren(ctx, ast)
        }
    }

    private fun processPattern(ctx: CommonContext, ast: AstInternalNode) {
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

    private fun processProperties(ctx: CommonContext, ast: AstInternalNode) {
        if (ctx.evaluateQuerySpecifics && ctx is QueryContext) {
            ctx.isFiltered = true
        }
        traverseChildren(ctx, ast)
    }

    private fun processNode(ctx: CommonContext, ast: AstInternalNode) {
        val node = getNode(ctx, ast)
        ctx.nodes.add(node)
        ctx.patternContext?.add(node)

        traverseChildren(ctx, ast)

        updateFilterStatusOfLastVar(ctx)
    }

    private fun processRelation(ctx: CommonContext, ast: AstInternalNode) {
        val relation = getRelation(ctx, ast)
        ctx.relations.add(relation)
        ctx.patternContext?.add(relation)

        traverseChildren(ctx, ast)

        updateFilterStatusOfLastVar(ctx)
    }

    private fun processWhere(ctx: CommonContext, ast: AstInternalNode) {
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
                varState.filterType = filterTypeHelper.mergeFilterTypes(varState.filterType, getFilterType(ctx))
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
                varState.returnType = returnTypeHelper.mergeReturnTypes(varState.returnType, getReturnType(ctx))
            }
            ctx.lastVar = null
            ctx.isAggregationFunction = false
        }
    }

    private fun processVariable(ctx: CommonContext, ast: AstLeafValue) {
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

    private fun processPropertyDotAccess(ctx: CommonContext, ast: AstInternalNode) {
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

}
