package at.jku.faw.symspace.cypherrewriter.core.cypher.enforcer

import at.jku.faw.symspace.cypherrewriter.core.cypher.*
import at.jku.faw.symspace.cypherrewriter.core.cypher.detector.Detection
import at.jku.faw.symspace.cypherrewriter.core.cypher.parser.CypherRewritingParserImpl
import at.jku.faw.symspace.cypherrewriter.core.cypher.unparser.CypherRewritingUnparserImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/*
 * This test class was generated using AI tools (Claude Code)
 */
class CypherEnforcerTest {

    private val parser = CypherRewritingParserImpl()
    private val unparser = CypherRewritingUnparserImpl()

    private val appContext = object : CypherAppContext {
        override var currentUsername: String = "123456"
    }

    private val permissionConfig = PermissionConfig(
        policies = emptyList(),
        filterTemplates = listOf(
            FilterTemplate(
                filterId = "ownerFilter",
                authorizationLevel = AuthorizationLevel.OWNER_LEVEL,
                pattern = "%s.id = %s",
                arguments = listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME)
            )
        )
    )

    private lateinit var enforcer: CypherEnforcerImpl

    @BeforeEach
    fun setUp() {
        enforcer = CypherEnforcerImpl(appContext, permissionConfig)
    }

    @Test
    fun enforceOnEmptyWhere() {
        assertEnforcement(
            input = "MATCH (u:User) RETURN u",
            expected = "MATCH (u:User) WHERE u.id = 123456 RETURN u",
            variable = "u"
        )
    }

    @Test
    fun enforceWithExistingOrExpression() {
        assertEnforcement(
            input = "MATCH (u:User) WHERE u.name = \"A\" OR u.name = \"B\" RETURN u",
            expected = "MATCH (u:User) WHERE (u.id = 123456) AND (u.name = \"A\" OR u.name = \"B\") RETURN u",
            variable = "u"
        )
    }

    @Test
    fun enforceWithExistingXorExpression() {
        assertEnforcement(
            input = "MATCH (u:User) WHERE u.name = \"A\" XOR u.name = \"B\" RETURN u",
            expected = "MATCH (u:User) WHERE (u.id = 123456) AND (u.name = \"A\" XOR u.name = \"B\") RETURN u",
            variable = "u"
        )
    }

    @Test
    fun enforceWithExistingAndExpression() {
        assertEnforcement(
            input = "MATCH (u:User) WHERE u.name = \"A\" AND u.age > 20 RETURN u",
            expected = "MATCH (u:User) WHERE (u.id = 123456) AND (u.name = \"A\" AND u.age > 20) RETURN u",
            variable = "u"
        )
    }

    @Test
    fun enforceWithComplexOrAndExpression() {
        assertEnforcement(
            input = "MATCH (u:User) WHERE u.a = 1 AND u.b = 2 OR u.c = 3 RETURN u",
            expected = "MATCH (u:User) WHERE (u.id = 123456) AND (u.a = 1 AND u.b = 2 OR u.c = 3) RETURN u",
            variable = "u"
        )
    }

    @Test
    fun enforceAirbnbRegressionCase() {
        assertEnforcement(
            input = "MATCH (var0:User{id: 24837397})-[var1]->() WHERE var1.alpha674 <> -553 OR var0.id <> 20815159 RETURN var0",
            expected = "MATCH (var0:User{id: 24837397})-[var1]->() WHERE (var0.id = 123456) AND (var1.alpha674 <> -553 OR var0.id <> 20815159) RETURN var0",
            variable = "var0"
        )
    }

    @Test
    fun enforceMultipleDetections() {
        val input = "MATCH (u:User)-[r]->(h:Host) WHERE u.name = \"A\" OR h.name = \"B\" RETURN u, h"
        val ast = parse(input)
        val matchNode = findNode(ast, AstType.MATCH)!!

        val detection1 = createDetection("u", matchNode)
        val detection2 = createDetection("h", matchNode)

        enforcer.enforce(listOf(detection1, detection2))

        val output = unparser.render(ast)
        assertEquals(
            "MATCH (u:User)-[r]->(h:Host) WHERE (h.id = 123456) AND ((u.id = 123456) AND (u.name = \"A\" OR h.name = \"B\")) RETURN u, h",
            output
        )
    }

    @Test
    fun publicLevelSkipsEnforcement() {
        val publicConfig = PermissionConfig(
            policies = emptyList(),
            filterTemplates = listOf(
                FilterTemplate("publicFilter", AuthorizationLevel.PUBLIC_LEVEL, "%s.id = %s",
                    listOf(ArgumentType.RESOURCE_VARIABLE, ArgumentType.USERNAME))
            )
        )
        val publicEnforcer = CypherEnforcerImpl(appContext, publicConfig)

        val input = "MATCH (u:User) WHERE u.name = \"A\" OR u.name = \"B\" RETURN u"
        val ast = parse(input)
        val matchNode = findNode(ast, AstType.MATCH)!!

        val rule = Rule("publicRule", "u", emptyList(), "publicFilter", AuthorizationLevel.PUBLIC_LEVEL)
        val detection = Detection(rule).apply {
            protectedNode = findNodeWithVariable(matchNode, "u")
            enforcementNode = matchNode
        }

        publicEnforcer.enforce(listOf(detection))

        val output = unparser.render(ast)
        assertEquals(input, output)
    }

    private fun assertEnforcement(input: String, expected: String, variable: String) {
        val ast = parse(input)
        val matchNode = findNode(ast, AstType.MATCH)!!
        val detection = createDetection(variable, matchNode)

        enforcer.enforce(listOf(detection))

        val output = unparser.render(ast)
        assertEquals(expected, output)
    }

    private fun createDetection(variable: String, matchNode: AstInternalNode): Detection {
        val rule = Rule("testRule", variable, emptyList(), "ownerFilter", AuthorizationLevel.OWNER_LEVEL)
        return Detection(rule).apply {
            protectedNode = findNodeWithVariable(matchNode, variable)
            enforcementNode = matchNode
        }
    }

    private fun parse(query: String): AstInternalNode {
        val parserInstance = parser.getParser(query)
        val context = parserInstance.oC_Cypher()
        return parser.parse(context) as AstInternalNode
    }

    private fun findNode(node: AstNode, type: AstType): AstInternalNode? {
        if (node.type == type) return node as AstInternalNode
        if (node is AstInternalNode) {
            for (child in node.elements) {
                val found = findNode(child, type)
                if (found != null) return found
            }
        }
        return null
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
}
