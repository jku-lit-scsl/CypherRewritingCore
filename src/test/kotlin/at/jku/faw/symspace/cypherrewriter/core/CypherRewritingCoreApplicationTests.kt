package at.jku.faw.symspace.cypherrewriter.core

import at.jku.faw.symspace.cypherrewriter.core.antlr.CypherParser
import at.jku.faw.symspace.cypherrewriter.core.cypher.parser.NewCypherParser
import at.jku.faw.symspace.cypherrewriter.core.cypher.parser.NewCypherParserImpl
import at.jku.faw.symspace.cypherrewriter.core.cypher.renderer.AstVisualizerImpl
import at.jku.faw.symspace.cypherrewriter.core.cypher.renderer.NewCypherRenderer
import at.jku.faw.symspace.cypherrewriter.core.cypher.renderer.NewCypherRendererImpl
import org.antlr.v4.runtime.ParserRuleContext
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ContextConfiguration(classes = [NewCypherParserImpl::class, NewCypherRendererImpl::class])
class AntlrKotlinTestApplicationTests(
    @Autowired private val parser: NewCypherParser,
    @Autowired private val renderer: NewCypherRenderer
) {

    @Test
    fun contextLoads() {
    }

    @Test
    fun testRelationshipPattern() {
        "<--".testEquality { it.oC_RelationshipPattern() }
        "--".testEquality { it.oC_RelationshipPattern() }
        "-->".testEquality { it.oC_RelationshipPattern() }
        "-[:TEST1]->".testEquality { it.oC_RelationshipPattern() }
        "<-[:TEST1]-".testEquality { it.oC_RelationshipPattern() }
        "-[:TEST1]-".testEquality { it.oC_RelationshipPattern() }
        "-[:TEST1|TEST2]->".testEquality { it.oC_RelationshipPattern() }
        "<-[:TEST1|TEST2]-".testEquality { it.oC_RelationshipPattern() }
        "-[:TEST1|TEST2]-".testEquality { it.oC_RelationshipPattern() }
        "-[:TEST1|TEST2|TEST3]-".testEquality { it.oC_RelationshipPattern() }
        "-[n:TEST]-".testEquality { it.oC_RelationshipPattern() }
        "-[n:TEST|TEST2]-".testEquality { it.oC_RelationshipPattern() }
        "-[n:TEST*]-".testEquality { it.oC_RelationshipPattern() }
        "-[n:TEST*1..]-".testEquality { it.oC_RelationshipPattern() }
        "-[n:TEST*1..3]-".testEquality { it.oC_RelationshipPattern() }
        "-[n:TEST*..3]-".testEquality { it.oC_RelationshipPattern() }
        "-[n:TEST|TEST2*]-".testEquality { it.oC_RelationshipPattern() }
        "-[n:TEST|TEST2*1..]-".testEquality { it.oC_RelationshipPattern() }
        "-[n:TEST|TEST2*..3]-".testEquality { it.oC_RelationshipPattern() }
        "-[n:TEST|TEST2*1..3]-".testEquality { it.oC_RelationshipPattern() }
    }

    @Test
    fun testRangeLiteral() {
        "-[*]-".testEquality { it.oC_RelationshipPattern() }
        "-[*1 ]-".testEquality("-[*1]-") { it.oC_RelationshipPattern() }
        "-[* 1]-".testEquality("-[*1]-") { it.oC_RelationshipPattern() }
        "-[*1..]-".testEquality { it.oC_RelationshipPattern() }
        "-[*1 ..]-".testEquality("-[*1..]-") { it.oC_RelationshipPattern() }
        "-[*1 .. ]-".testEquality("-[*1..]-") { it.oC_RelationshipPattern() }
        "-[*1 .. 2]-".testEquality("-[*1..2]-") { it.oC_RelationshipPattern() }
        "-[*1..2]-".testEquality { it.oC_RelationshipPattern() }
        "-[*1.. 2]-".testEquality("-[*1..2]-") { it.oC_RelationshipPattern() }
        "-[*.. 2]-".testEquality("-[*..2]-") { it.oC_RelationshipPattern() }
        "-[*..2]-".testEquality { it.oC_RelationshipPattern() }
        "-[*..]-".testEquality("-[*]-") { it.oC_RelationshipPattern() }
    }

    @Test
    fun testFunctionInvocation() {
        "sum(e.test)".testEquality { it.oC_FunctionInvocation() }
        "sum(a.b, c.d)".testEquality { it.oC_FunctionInvocation() }
    }

    @Test
    fun testNode() {
        "()".testEquality { it.oC_NodePattern() }

        "(s)".testEquality { it.oC_NodePattern() }
        "(var)".testEquality { it.oC_NodePattern() }

        "(var:Label1)".testEquality { it.oC_NodePattern() }
        "(var:Label1:Label2)".testEquality { it.oC_NodePattern() }

        "(var:Label1:Label2{})".testEquality { it.oC_NodePattern() }
        "(var:Label1:Label2{prop: 1})".testEquality { it.oC_NodePattern() }

        "(var:Label1:Label2{prop: \"value\"})".testEquality { it.oC_NodePattern() }
        "(var:Label1:Label2{prop: \"value\", prop2: 42})".testEquality { it.oC_NodePattern() }

        "(var:Label1{})".testEquality { it.oC_NodePattern() }
        "(var{})".testEquality { it.oC_NodePattern() }
    }

    @Test
    fun testQueries() {
        "MATCH () RETURN s".testEquality { it.oC_Cypher() }
        "MATCH (s) RETURN *".testEquality { it.oC_Cypher() }
        "MATCH (s:Label) RETURN *".testEquality { it.oC_Cypher() }
        "MATCH (s:Label{prop: 42}) RETURN *".testEquality { it.oC_Cypher() }
        "MATCH (p:Professor)-[:READS]->(l)--(s:Student{study: \"Computer Science\"}) WHERE l:Lecture AND p.name = \"Josef\" RETURN s, l, p".testEquality { it.oC_Cypher() }
        "MATCH (a)-[b]-(c) WHERE a:EDGE:TEST RETURN *".testEquality { it.oC_Cypher() }
        "MATCH (a)-[b]-(c) WHERE b:EDGE OR b:TEST RETURN *".testEquality { it.oC_Cypher() }
    }

    private fun assertEquals(input: String, expected: String = input, function: (CypherParser) -> ParserRuleContext) {
        val context = function(parser.getParser(input))
        val ast = parser.parse(context)
        val output = renderer.render(ast)
        Assertions.assertEquals(expected, output)
    }

    private fun String.testEquality(expected: String = this, function: (CypherParser) -> ParserRuleContext) {
        assertEquals(this, expected, function)
    }


}
