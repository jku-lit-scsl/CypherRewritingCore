package at.jku.faw.symspace.cypherrewriter.core.cypher.detector

import at.jku.faw.symspace.cypherrewriter.core.cypher.*
import at.jku.faw.symspace.cypherrewriter.core.cypher.parser.CypherRewritingParserImpl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/*
 * This test class was generated using AI tools (Claude Code)
 */
class PermissionDetectorTest {

    private val parser = CypherRewritingParserImpl()
    private val returnTypeHelper: ReturnTypeHelper = ReturnTypeHelperImpl()
    private val filterTypeHelper: FilterTypeHelper = FilterTypeHelperImpl()
    private val labelMatcher: LabelMatcher = LabelMatcherImpl()
    private val ruleHelper: RuleHelper = RuleHelperImpl(filterTypeHelper, returnTypeHelper)

    private fun createDetector(config: PermissionConfig): PermissionDetector {
        return PermissionDetectorImpl(parser, returnTypeHelper, filterTypeHelper, labelMatcher, ruleHelper, config)
    }

    private fun parse(query: String): AstInternalNode {
        val parserInstance = parser.getParser(query)
        val context = parserInstance.oC_Cypher()
        return parser.parse(context) as AstInternalNode
    }

    private fun findNodeWithVariable(node: AstInternalNode, variableName: String): AstInternalNode? {
        for (child in node.elements) {
            if (child is AstInternalNode) {
                val variable = child.elements.find {
                    it is AstLeafValue && it.type == AstType.VARIABLE && it.value == variableName
                }
                if (variable != null && child.type in AstMetadata.SET_OF_VARIABLE_STORABLE_AST_TYPES) {
                    return child
                }
                val found = findNodeWithVariable(child, variableName)
                if (found != null) return found
            }
        }
        return null
    }

    // Simple node match: label matches

    @Test
    fun simpleNodeMatchLabelMatches() {
        val config = PermissionConfigBuilder()
            .policy()
            .pattern("(u:User)")
            .varMatching("u", LabelMatchStrategy.EXACT, false)
            .rule("rule1", "u", "filter1", AuthorizationLevel.OWNER_LEVEL)
            .on(ConditionExpression("u", FilterType.ANY, ReturnType.ANY))
            .endPattern()
            .filter("filter1", AuthorizationLevel.OWNER_LEVEL, "%s.username = %s",
                listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME))
            .build()

        val detector = createDetector(config)
        val ast = parse("MATCH (u:User) RETURN u")
        val detections = detector.process(ast)

        assertEquals(1, detections.size)
        assertEquals("rule1", detections[0].rule.id)
        assertEquals(AuthorizationLevel.OWNER_LEVEL, detections[0].authorizationLevel)
    }

    // Simple node match: label does not match

    @Test
    fun simpleNodeMatchLabelDoesNotMatch() {
        val config = PermissionConfigBuilder()
            .policy()
            .pattern("(u:Admin)")
            .varMatching("u", LabelMatchStrategy.EXACT, false)
            .rule("rule1", "u", "filter1", AuthorizationLevel.OWNER_LEVEL)
            .on(ConditionExpression("u", FilterType.ANY, ReturnType.ANY))
            .endPattern()
            .filter("filter1", AuthorizationLevel.OWNER_LEVEL, "%s.username = %s",
                listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME))
            .build()

        val detector = createDetector(config)
        val ast = parse("MATCH (u:User) RETURN u")
        val detections = detector.process(ast)

        assertTrue(detections.isEmpty())
    }

    // matchEmptyLabels=true with label-less query node

    @Test
    fun matchEmptyLabelsTrueMatchesLabellessNode() {
        val config = PermissionConfigBuilder()
            .policy()
            .pattern("(u:User)")
            .varMatching("u", LabelMatchStrategy.EXACT, true)
            .rule("rule1", "u", "filter1", AuthorizationLevel.OWNER_LEVEL)
            .on(ConditionExpression("u", FilterType.ANY, ReturnType.ANY))
            .endPattern()
            .filter("filter1", AuthorizationLevel.OWNER_LEVEL, "%s.username = %s",
                listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME))
            .build()

        val detector = createDetector(config)
        val ast = parse("MATCH (u) RETURN u")
        val detections = detector.process(ast)

        assertEquals(1, detections.size)
    }

    // matchEmptyLabels=false with label-less query node
    @Test
    fun matchEmptyLabelsFalseDoesNotMatchLabellessNode() {
        val config = PermissionConfigBuilder()
            .policy()
            .pattern("(u:User)")
            .varMatching("u", LabelMatchStrategy.EXACT, false)
            .rule("rule1", "u", "filter1", AuthorizationLevel.OWNER_LEVEL)
            .on(ConditionExpression("u", FilterType.ANY, ReturnType.ANY))
            .endPattern()
            .filter("filter1", AuthorizationLevel.OWNER_LEVEL, "%s.username = %s",
                listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME))
            .build()

        val detector = createDetector(config)
        val ast = parse("MATCH (u) RETURN u")
        val detections = detector.process(ast)

        assertTrue(detections.isEmpty())
    }

    // Relationship pattern recognition

    @Test
    fun relationshipPatternDetection() {
        val config = PermissionConfigBuilder()
            .policy()
            .pattern("(u:User)-[r:OWNS]->(h:Host)")
            .varMatching("u", LabelMatchStrategy.EXACT, false)
            .varMatching("h", LabelMatchStrategy.EXACT, false)
            .rule("rule1", "u", "filter1", AuthorizationLevel.OWNER_LEVEL)
            .on(ConditionExpression("u", FilterType.ANY, ReturnType.ANY))
            .endPattern()
            .filter("filter1", AuthorizationLevel.OWNER_LEVEL, "%s.username= %s",
                listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME))
            .build()

        val detector = createDetector(config)
        val ast = parse("MATCH (u:User)-[r:OWNS]->(h:Host) RETURN u")
        val detections = detector.process(ast)

        assertEquals(1, detections.size)
        assertEquals("rule1", detections[0].rule.id)
    }

    // Variable with RETURNED_AS_VALUE (RETURN u)

    @Test
    fun variableReturnedAsValue() {
        val config = PermissionConfigBuilder()
            .policy()
            .pattern("(u:User)")
            .varMatching("u", LabelMatchStrategy.EXACT, false)
            .rule("rule1", "u", "filter1", AuthorizationLevel.OWNER_LEVEL)
            .on(ConditionExpression("u", FilterType.ANY, ReturnType.RETURNED_AS_VALUE))
            .endPattern()
            .filter("filter1", AuthorizationLevel.OWNER_LEVEL, "%s.username= %s",
                listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME))
            .build()

        val detector = createDetector(config)
        val ast = parse("MATCH (u:User) RETURN u")
        val detections = detector.process(ast)

        assertEquals(1, detections.size)
    }

    // Variable with NO_RETURN (not returned)

    @Test
    fun variableNotReturned() {
        val config = PermissionConfigBuilder()
            .policy()
            .pattern("(u:User)")
            .varMatching("u", LabelMatchStrategy.EXACT, false)
            .rule("rule1", "u", "filter1", AuthorizationLevel.OWNER_LEVEL)
            .on(ConditionExpression("u", FilterType.ANY, ReturnType.NO_RETURN))
            .endPattern()
            .filter("filter1", AuthorizationLevel.OWNER_LEVEL, "%s.username= %s",
                listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME))
            .build()

        val detector = createDetector(config)
        // u is returned, so NO_RETURN condition should not match
        val ast = parse("MATCH (u:User) RETURN u")
        val detections = detector.process(ast)

        assertTrue(detections.isEmpty())
    }

    // Variable FILTERED (WHERE u.name = ...)

    @Test
    fun variableFiltered() {
        val config = PermissionConfigBuilder()
            .policy()
            .pattern("(u:User)")
            .varMatching("u", LabelMatchStrategy.EXACT, false)
            .rule("rule1", "u", "filter1", AuthorizationLevel.OWNER_LEVEL)
            .on(ConditionExpression("u", FilterType.FILTERED, ReturnType.ANY))
            .endPattern()
            .filter("filter1", AuthorizationLevel.OWNER_LEVEL, "%s.username= %s",
                listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME))
            .build()

        val detector = createDetector(config)
        val ast = parse("MATCH (u:User) WHERE u.name = \"Test\" RETURN u")
        val detections = detector.process(ast)

        assertEquals(1, detections.size)
    }

    // Variable NO_FILTER (no WHERE)

    @Test
    fun variableNoFilter() {
        val config = PermissionConfigBuilder()
            .policy()
            .pattern("(u:User)")
            .varMatching("u", LabelMatchStrategy.EXACT, false)
            .rule("rule1", "u", "filter1", AuthorizationLevel.OWNER_LEVEL)
            .on(ConditionExpression("u", FilterType.NO_FILTER, ReturnType.ANY))
            .endPattern()
            .filter("filter1", AuthorizationLevel.OWNER_LEVEL, "%s.username= %s",
                listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME))
            .build()

        val detector = createDetector(config)
        val ast = parse("MATCH (u:User) RETURN u")
        val detections = detector.process(ast)

        assertEquals(1, detections.size)
    }

    // Property in node {name: "Test"} -> FILTERED

    @Test
    fun propertyInNodeMakesVariableFiltered() {
        val config = PermissionConfigBuilder()
            .policy()
            .pattern("(u:User)")
            .varMatching("u", LabelMatchStrategy.EXACT, false)
            .rule("rule1", "u", "filter1", AuthorizationLevel.OWNER_LEVEL)
            .on(ConditionExpression("u", FilterType.FILTERED, ReturnType.ANY))
            .endPattern()
            .filter("filter1", AuthorizationLevel.OWNER_LEVEL, "%s.username= %s",
                listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME))
            .build()

        val detector = createDetector(config)
        val ast = parse("MATCH (u:User{name: \"Test\"}) RETURN u")
        val detections = detector.process(ast)

        assertEquals(1, detections.size)
    }

    // RETURN * -> all variables RETURNED_AS_VALUE

    @Test
    fun returnAsteriskMakesAllVariablesReturnedAsValue() {
        val config = PermissionConfigBuilder()
            .policy()
            .pattern("(u:User)")
            .varMatching("u", LabelMatchStrategy.EXACT, false)
            .rule("rule1", "u", "filter1", AuthorizationLevel.OWNER_LEVEL)
            .on(ConditionExpression("u", FilterType.ANY, ReturnType.RETURNED_AS_VALUE))
            .endPattern()
            .filter("filter1", AuthorizationLevel.OWNER_LEVEL, "%s.username= %s",
                listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME))
            .build()

        val detector = createDetector(config)
        val ast = parse("MATCH (u:User) RETURN *")
        val detections = detector.process(ast)

        assertEquals(1, detections.size)
    }

    // Aggregation sum(u.score) -> AGGREGATED

    @Test
    fun aggregationFunctionSetsAggregatedReturnType() {
        val config = PermissionConfigBuilder()
            .policy()
            .pattern("(u:User)")
            .varMatching("u", LabelMatchStrategy.EXACT, false)
            .rule("rule1", "u", "filter1", AuthorizationLevel.OWNER_LEVEL)
            .on(ConditionExpression("u", FilterType.ANY, ReturnType.AGGREGATED))
            .endPattern()
            .filter("filter1", AuthorizationLevel.OWNER_LEVEL, "%s.username= %s",
                listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME))
            .build()

        val detector = createDetector(config)
        val ast = parse("MATCH (u:User) RETURN sum(u.score)")
        val detections = detector.process(ast)

        assertEquals(1, detections.size)
    }

    // OR-Condition with two AND-Branches

    @Test
    fun orConditionWithTwoAndBranches() {
        val and1 = ConditionCombination(
            ConditionBoolean.AND,
            ConditionExpression("u", FilterType.FILTERED, ReturnType.ANY),
            ConditionExpression("u", FilterType.ANY, ReturnType.RETURNED_AS_VALUE)
        )
        val and2 = ConditionCombination(
            ConditionBoolean.AND,
            ConditionExpression("u", FilterType.ANY, ReturnType.NO_RETURN),
            ConditionExpression("u", FilterType.NO_FILTER, ReturnType.ANY)
        )
        val orCondition = ConditionCombination(ConditionBoolean.OR, and1, and2)

        val config = PermissionConfigBuilder()
            .policy()
            .pattern("(u:User)")
            .varMatching("u", LabelMatchStrategy.EXACT, false)
            .rule("rule1", "u", "filter1", AuthorizationLevel.OWNER_LEVEL)
            .on(orCondition)
            .endPattern()
            .filter("filter1", AuthorizationLevel.OWNER_LEVEL, "%s.username= %s",
                listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME))
            .build()

        val detector = createDetector(config)

        // First AND branch should match: u is FILTERED and RETURNED_AS_VALUE
        val ast1 = parse("MATCH (u:User) WHERE u.name = \"A\" RETURN u")
        val detections1 = detector.process(ast1)
        assertEquals(1, detections1.size)

        // Second AND branch should match: u has NO_RETURN and NO_FILTER
        // Need a query where u is neither returned nor filtered
        // Since the query must be valid Cypher with a RETURN clause, use another variable
        val ast2 = parse("MATCH (u:User)-[r]->(h:Host) RETURN h")
        val config2 = PermissionConfigBuilder()
            .policy()
            .pattern("(u:User)-[r]->(h:Host)")
            .varMatching("u", LabelMatchStrategy.EXACT, false)
            .varMatching("h", LabelMatchStrategy.EXACT, false)
            .rule("rule1", "u", "filter1", AuthorizationLevel.OWNER_LEVEL)
            .on(ConditionCombination(
                ConditionBoolean.AND,
                ConditionExpression("u", FilterType.NO_FILTER, ReturnType.NO_RETURN)
            ))
            .endPattern()
            .filter("filter1", AuthorizationLevel.OWNER_LEVEL, "%s.username= %s",
                listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME))
            .build()
        val detector2 = createDetector(config2)
        val detections2 = detector2.process(ast2)
        assertEquals(1, detections2.size)
    }

    // Multiple rules per policy -> multiple detections

    @Test
    fun multipleRulesPerPolicyMultipleDetections() {
        val config = PermissionConfigBuilder()
            .policy()
            .pattern("(u:User)-[r]->(h:Host)")
            .varMatching("u", LabelMatchStrategy.EXACT, false)
            .varMatching("h", LabelMatchStrategy.EXACT, false)
            .rule("rule1", "u", "filter1", AuthorizationLevel.OWNER_LEVEL)
            .on(ConditionExpression("u", FilterType.ANY, ReturnType.ANY))
            .rule("rule2", "h", "filter1", AuthorizationLevel.AUTHORIZED_LEVEL)
            .on(ConditionExpression("h", FilterType.ANY, ReturnType.ANY))
            .endPattern()
            .filter("filter1", AuthorizationLevel.OWNER_LEVEL, "%s.username= %s",
                listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME))
            .build()

        val detector = createDetector(config)
        val ast = parse("MATCH (u:User)-[r]->(h:Host) RETURN u, h")
        val detections = detector.process(ast)

        assertEquals(2, detections.size)
        val ruleIds = detections.map { it.rule.id }.toSet()
        assertTrue(ruleIds.contains("rule1"))
        assertTrue(ruleIds.contains("rule2"))
    }

    // No match -> empty list

    @Test
    fun noMatchReturnsEmptyList() {
        val config = PermissionConfigBuilder()
            .policy()
            .pattern("(u:Admin)")
            .varMatching("u", LabelMatchStrategy.EXACT, false)
            .rule("rule1", "u", "filter1", AuthorizationLevel.OWNER_LEVEL)
            .on(ConditionExpression("u", FilterType.ANY, ReturnType.ANY))
            .endPattern()
            .filter("filter1", AuthorizationLevel.OWNER_LEVEL, "%s.username= %s",
                listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME))
            .build()

        val detector = createDetector(config)
        val ast = parse("MATCH (u:User) RETURN u")
        val detections = detector.process(ast)

        assertTrue(detections.isEmpty())
    }

    // Standalone policy node with matchEmptyLabels=true detects to-node in path

    @Test
    fun standaloneNodePolicyDetectsToNodeInLeftDirectedPathWhenFromNodeAlsoMatches() {
        // Regression test: MATCH (var0)<-[var2]-(var1) RETURN var0
        // Path is stored as Path(from=var1, rel=var2, to=var0).
        // Direct structural match maps l→var1 (from-node, NOT returned).
        // Before fix: else-if prevented projected match → var0 never tried → no detection.
        // After fix: projected match also tried → l→var0 (returned) → detection fires.
        val config = PermissionConfigBuilder()
            .policy()
            .pattern("(l:Listing)")
            .varMatching("l", LabelMatchStrategy.CONTAINS_ANY, true)
            .rule("rule1", "l", "filter1", AuthorizationLevel.OWNER_LEVEL)
            .on(ConditionExpression("l", FilterType.ANY, ReturnType.RETURNED_AS_VALUE))
            .endPattern()
            .filter("filter1", AuthorizationLevel.OWNER_LEVEL, "(:Host {id: %s})-[:HOSTS]->(%s)",
                listOf(ArgumentType.USERNAME, ArgumentType.RESOURCE_VARIABLE))
            .build()

        val detector = createDetector(config)
        val ast = parse("MATCH (var0)<-[var2]-(var1) RETURN var0, var2 LIMIT 48")
        val detections = detector.process(ast)

        assertEquals(1, detections.size)
        assertEquals("rule1", detections[0].rule.id)
    }

    // Multiple policies

    @Test
    fun multiplePoliciesBothMatch() {
        val config = PermissionConfigBuilder()
            .policy()
            .pattern("(u:User)")
            .varMatching("u", LabelMatchStrategy.EXACT, false)
            .rule("rule1", "u", "filter1", AuthorizationLevel.OWNER_LEVEL)
            .on(ConditionExpression("u", FilterType.ANY, ReturnType.ANY))
            .endPattern()
            .policy()
            .pattern("(h:Host)")
            .varMatching("h", LabelMatchStrategy.EXACT, false)
            .rule("rule2", "h", "filter1", AuthorizationLevel.AUTHORIZED_LEVEL)
            .on(ConditionExpression("h", FilterType.ANY, ReturnType.ANY))
            .endPattern()
            .filter("filter1", AuthorizationLevel.OWNER_LEVEL, "%s.username= %s",
                listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME))
            .build()

        val detector = createDetector(config)
        val ast = parse("MATCH (u:User)-[r]->(h:Host) RETURN u, h")
        val detections = detector.process(ast)

        assertEquals(2, detections.size)
        val ruleIds = detections.map { it.rule.id }.toSet()
        assertTrue(ruleIds.contains("rule1"))
        assertTrue(ruleIds.contains("rule2"))
    }

    @Test
    fun multiplePoliciesOnlyOneMatches() {
        val config = PermissionConfigBuilder()
            .policy()
            .pattern("(u:User)")
            .varMatching("u", LabelMatchStrategy.EXACT, false)
            .rule("rule1", "u", "filter1", AuthorizationLevel.OWNER_LEVEL)
            .on(ConditionExpression("u", FilterType.ANY, ReturnType.ANY))
            .endPattern()
            .policy()
            .pattern("(a:Admin)")
            .varMatching("a", LabelMatchStrategy.EXACT, false)
            .rule("rule2", "a", "filter1", AuthorizationLevel.AUTHORIZED_LEVEL)
            .on(ConditionExpression("a", FilterType.ANY, ReturnType.ANY))
            .endPattern()
            .filter("filter1", AuthorizationLevel.OWNER_LEVEL, "%s.username= %s",
                listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME))
            .build()

        val detector = createDetector(config)
        val ast = parse("MATCH (u:User) RETURN u")
        val detections = detector.process(ast)

        assertEquals(1, detections.size)
        assertEquals("rule1", detections[0].rule.id)
    }
}
