package at.jku.faw.symspace.cypherrewriter.core.cypher.parser

import at.jku.faw.symspace.cypherrewriter.core.antlr.CypherParser
import at.jku.faw.symspace.cypherrewriter.core.cypher.unparser.CypherRewritingUnparser
import at.jku.faw.symspace.cypherrewriter.core.cypher.unparser.CypherRewritingUnparserImpl
import org.antlr.v4.runtime.ParserRuleContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ContextConfiguration(classes = [CypherRewritingParserImpl::class, CypherRewritingUnparserImpl::class])
class CypherRewritingVisitorTest(
    @Autowired private val parser: CypherRewritingParser,
    @Autowired private val renderer: CypherRewritingUnparser
) {

    @Test
    fun contextLoads() {
    }

    @Test
    fun visitOC_Cypher() {
        "MATCH () RETURN s".testEquality { it.oC_Cypher() }
        "MATCH (s) RETURN *".testEquality { it.oC_Cypher() }
        "MATCH (s:Label) RETURN *".testEquality { it.oC_Cypher() }
        "MATCH (s:Label{prop: 42}) RETURN *".testEquality { it.oC_Cypher() }
        "MATCH (p:Professor)-[:READS]->(l)--(s:Student{study: \"Computer Science\"}) WHERE l:Lecture AND p.name = \"Josef\" RETURN s, l, p".testEquality { it.oC_Cypher() }
        "MATCH (a)-[b]-(c) WHERE a:EDGE:TEST RETURN *".testEquality { it.oC_Cypher() }
        "MATCH (a)-[b]-(c) WHERE b:EDGE OR b:TEST RETURN *".testEquality { it.oC_Cypher() }
    }

    @Test
    fun visitOC_SinglePartQuery() {
    }

    @Test
    fun visitOC_MultiPartQuery() {
    }

    @Test
    fun visitOC_MultiPartQueryGroup() {
    }

    @Test
    fun visitOC_Match() {
    }

    @Test
    fun visitOC_PatternPart() {
    }

    @Test
    fun visitOC_NodePattern() {
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
    fun visitOC_LabelName() {
    }

    @Test
    fun visitOC_Parameter() {
    }

    @Test
    fun visitOC_PropertyKeyName() {
    }

    @Test
    fun visitOC_MapLiteral() {
    }

    @Test
    fun visitOC_Return() {
    }

    @Test
    fun visitOC_ProjectionBody() {
    }

    @Test
    fun visitOC_ProjectionItems() {
    }

    @Test
    fun visitOC_Order() {
    }

    @Test
    fun visitOC_SortItem() {
    }

    @Test
    fun visitOC_Skip() {
    }

    @Test
    fun visitOC_Limit() {
    }

    @Test
    fun visitOC_ProjectionItem() {
    }

    @Test
    fun visitOC_Variable() {
    }

    @Test
    fun visitOC_OrExpression() {
    }

    @Test
    fun visitOC_XorExpression() {
    }

    @Test
    fun visitOC_AndExpression() {
    }

    @Test
    fun visitOC_NotExpression() {
    }

    @Test
    fun visitOC_RelationshipPattern() {
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
    fun visitOC_RelTypeName() {
    }

    @Test
    fun visitOC_RangeLiteral() {
        // Because of the way ranges are processed, they cannot be properly tested on its own.
        // Therefore, they are tested as part of RelationShipPattern.
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
    fun visitOC_DoubleLiteral() {
    }

    @Test
    fun visitOC_IntegerLiteral() {
    }

    @Test
    fun visitOC_Literal() {
    }

    @Test
    fun visitOC_FunctionInvocation() {
        "sum(e.test)".testEquality { it.oC_FunctionInvocation() }
        "sum(a.b, c.d)".testEquality { it.oC_FunctionInvocation() }
    }

    @Test
    fun visitOC_FunctionName() {
    }

    @Test
    fun visitOC_ParenthesizedExpression() {
    }

    @Test
    fun visitOC_Where() {
    }

    @Test
    fun visitOC_PropertyOrLabelsExpression() {
    }

    @Test
    fun visitOC_PartialComparisonExpression() {
    }

    private fun assertEquals(input: String, expected: String = input, function: (CypherParser) -> ParserRuleContext) {
        val context = function(parser.getParser(input))
        val ast = parser.parse(context)
        val output = renderer.render(ast)
        assertEquals(expected, output)
    }

    private fun String.testEquality(expected: String = this, function: (CypherParser) -> ParserRuleContext) {
        assertEquals(this, expected, function)
    }

}
