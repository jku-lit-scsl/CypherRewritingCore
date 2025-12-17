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
        "MATCH (node) RETURN node SKIP 42".testEquality { it.oC_Cypher() }
        "MATCH (node) RETURN node ORDER BY node.prop".testEquality { it.oC_Cypher() }
        "MATCH (node) RETURN NOT node ORDER BY node.prop".testEquality { it.oC_Cypher() }
        "MATCH (node) WHERE NOT node.prop RETURN NOT node ORDER BY node.prop".testEquality { it.oC_Cypher() }
    }

    @Test
    fun visitOC_Cypher_fuzzGenerated() {
        "MATCH (warragal:`ramekin`), (swathed:`ramekin`), (deride:`racquets`)-[demons:`halacha`]-(funks), (hurdlers:`ramekin`)-[charts:`coping`]-(mortally:`racquets`) WHERE ((((swathed.waffler <= -594195215 OR mortally.facias <= '1') AND hurdlers.dreams <> false AND charts.spinal < 'j') XOR charts.spinal <> '<') OR charts.spinal > 'Z') RETURN deride, mortally, charts, demons".testEquality { it.oC_Cypher() }
        "MATCH (bloke:`reperks`), (scolders:`retrieve`), (chaises:`reperks`)<-[woolfell:`pothers`]-(bloke), (ousels:`reperks`)-[wheeping:`babying`]->(notch:`reperks`) WHERE ((((bloke.trapunto CONTAINS 'toady' AND chaises.tombing <= -2899261474142396236) OR ousels.mints = true) XOR wheeping.loping >= -925950374) OR wheeping.college > '?' OR wheeping.bourg = -1412237780) RETURN scolders, bloke, woolfell, wheeping".testEquality { it.oC_Cypher() }
        "MATCH (wieldier:`etagere`), (rezoning:`hiccuped`), (effuses)-[figurant:`jasper`]->(rodmen), (redpoll:`etagere`)-[flanger:`tuneable`]-(aquiline) WHERE (((((redpoll.adeems <> 428725218 AND wieldier.yarmelke <> -2.7140233E38) OR redpoll.yarmelke <> -2.6161173E38) XOR figurant.craters >= 986838873746905038) AND figurant.craters >= -7695735531853757076) XOR flanger.wiled < 'g') RETURN wieldier, rezoning, flanger, figurant LIMIT 1586475128".testEquality { it.oC_Cypher() }
        "MATCH (b) WHERE test.call RETURN b".testEquality { it.oC_Cypher() }
        "MATCH (bumble:`sulphate`), (feckly:`pelorus`), (pederast:`sulphate`)<-[restamp:`lustre`]-(feckly), (diestrum)-[crescent:`etuis`]->(desands:`thing`) WHERE (((((desands.beslime STARTS WITH 'routine' OR bumble.call < -1.36188915562429E308) AND pederast.easier = true) XOR restamp.soul = 44) OR crescent.unwary >= -2.846071929815929E307) XOR restamp.soul <= -41) RETURN bumble, pederast, restamp, crescent".testEquality { it.oC_Cypher() }
        "MATCH (pusses:`matzahs`), (paludism:`matzahs`), (ectomere)-[natters:`slushes`]-(do), (paludism)-[rasping:`maizes`]->(pusses) WHERE ((((paludism.frizz < -8 OR pusses.entwined CONTAINS 'pitapat') AND paludism.caned = 'slimes') OR natters.dunch = false) AND natters.dunch = false AND natters.meddling > -14) RETURN paludism, paludism, natters, rasping LIMIT 1807050382".testEquality { it.oC_Cypher() }
        "MATCH (sipped:`garlicky`), (graving:`tidiness`), (end)-[fimbles:`zagged`]-(lazaret), (capelans)-[callused:`clothe`]-(learning) WHERE (((((graving.hasp < -1.5120243679729308E308 AND graving.declined <= -2.5527757E38) OR sipped.seism <= 'l') AND callused.gripier < -2.122774898867307E307) XOR callused.gradual >= 83) AND fimbles.alcohols < -326267101858240268) RETURN sipped, graving, fimbles, callused".testEquality { it.oC_Cypher() }
        "MATCH (ogham:`omegas`), (filmset:`omegas`), (filmset)<-[haole:`triaging`]-(skip:`gonophs`), (filmset)-[reposing:`redrives`]->(ogham) WHERE ((((filmset.dialings > 1.2780864E38 OR filmset.unknowns > -94) AND filmset.fouette > -22 AND reposing.kickup <> false) XOR haole.spurries = 9.522248075720658E307) OR reposing.kickup <> false) RETURN filmset, filmset, reposing, haole".testEquality { it.oC_Cypher() }
        "MATCH (tuxedo:`logics`), (dotation:`logics`), (teamed:`logics`)<-[sliced:`collard`]-(sealant:`hommoses`), (ends)-[upas:`thyrse`]-(repaints) WHERE ((((sealant.angel = -2.0112025E38 OR teamed.pharisee = true) AND dotation.pharisee <> true) XOR upas.melanges <= '8' XOR sliced.dossel >= 1699412816) AND sliced.deferred <= 120) RETURN tuxedo, teamed, upas, sliced".testEquality { it.oC_Cypher() }
        "MATCH (asserted:`cystitis`), (aniseeds:`mollahs`), (cajones)<-[wrassles:`struggle`]-(auxesis), (asserted)-[bedsonia:`carious`]->(ubique:`bidental`) WHERE (((aniseeds.end >= -1.3259025231961024E307 XOR ubique.kiddy < -5.073832086824236E307) AND aniseeds.moonrise = false) OR bedsonia.donga > 2.296765E38 OR wrassles.offals = 'Y' OR wrassles.asbestic <= '>') RETURN asserted, ubique, bedsonia, wrassles".testEquality { it.oC_Cypher() }
        "MATCH (n) WHERE n.end RETURN n".testEquality { it.oC_Cypher() }
        "MATCH (cobras:`subtext`), (undated:`sorter`), (undated)-[mayflies:`disbosom`]->(appetite), (cobras)-[manhood:`tranced`]-(rouches:`interns`) WHERE ((((cobras.beknight = false XOR rouches.nearlier > 6234878862368505601 XOR undated.yield < '\\'') OR mayflies.wiss < -1.5417794E38) AND mayflies.wiss = -2.1129001E38) OR manhood.testify <= 1068555902) RETURN cobras, rouches, mayflies, manhood".testEquality { it.oC_Cypher() }
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
    fun visitOC_UnaryAddOrSubtractExpression() {
        "-594195215".testEquality { it.oC_UnaryAddOrSubtractExpression() }
        "- 594195215".testEquality("-594195215") { it.oC_UnaryAddOrSubtractExpression() }
    }

    @Test
    fun visitOC_IntegerLiteral() {
        "594195215".testEquality { it.oC_IntegerLiteral() }
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
    fun visitOC_BooleanLiteral() {
        "true".testEquality { it.oC_BooleanLiteral() }
        "false".testEquality { it.oC_BooleanLiteral() }
        "True".testEquality("true") { it.oC_BooleanLiteral() }
        "False".testEquality("false") { it.oC_BooleanLiteral() }
        "TRUE".testEquality("true") { it.oC_BooleanLiteral() }
        "FALSE".testEquality("false") { it.oC_BooleanLiteral() }
        "TrUe".testEquality("true") { it.oC_BooleanLiteral() }
        "FaLsE".testEquality("false") { it.oC_BooleanLiteral() }
        "tRuE".testEquality("true") { it.oC_BooleanLiteral() }
        "fAlSe".testEquality("false") { it.oC_BooleanLiteral() }
    }

    @Test
    fun visitOC_ComparisonExpression() {
        "0 < 0".testEquality { it.oC_ComparisonExpression() }
        "0 > 0".testEquality { it.oC_ComparisonExpression() }
        "0 >= 0".testEquality { it.oC_ComparisonExpression() }
        "0 <= 0".testEquality { it.oC_ComparisonExpression() }
        "0 <> 0".testEquality { it.oC_ComparisonExpression() }
        "0 = 0".testEquality { it.oC_ComparisonExpression() }

        "0  <  0".testEquality("0 < 0") { it.oC_ComparisonExpression() }
        "0  >  0".testEquality("0 > 0") { it.oC_ComparisonExpression() }
        "0  >=  0".testEquality("0 >= 0") { it.oC_ComparisonExpression() }
        "0  <=  0".testEquality("0 <= 0") { it.oC_ComparisonExpression() }
        "0  <>  0".testEquality("0 <> 0") { it.oC_ComparisonExpression() }
        "0  =  0".testEquality("0 = 0") { it.oC_ComparisonExpression() }

        "0<0".testEquality("0 < 0") { it.oC_ComparisonExpression() }
        "0>0".testEquality("0 > 0") { it.oC_ComparisonExpression() }
        "0>=0".testEquality("0 >= 0") { it.oC_ComparisonExpression() }
        "0<=0".testEquality("0 <= 0") { it.oC_ComparisonExpression() }
        "0<>0".testEquality("0 <> 0") { it.oC_ComparisonExpression() }
        "0=0".testEquality("0 = 0") { it.oC_ComparisonExpression() }

        "a starts with b".testEquality("a STARTS WITH b") { it.oC_ComparisonExpression() }
        "a  starts with b".testEquality("a STARTS WITH b") { it.oC_ComparisonExpression() }
        "a starts  with b".testEquality("a STARTS WITH b") { it.oC_ComparisonExpression() }
        "a starts with  b".testEquality("a STARTS WITH b") { it.oC_ComparisonExpression() }

        "a contains b".testEquality("a CONTAINS b") { it.oC_ComparisonExpression() }
        "a  contains b".testEquality("a CONTAINS b") { it.oC_ComparisonExpression() }
        "a contains b".testEquality("a CONTAINS b") { it.oC_ComparisonExpression() }
        "a contains  b".testEquality("a CONTAINS b") { it.oC_ComparisonExpression() }

        "a ends with b".testEquality("a ENDS WITH b") { it.oC_ComparisonExpression() }
        "a  ends with b".testEquality("a ENDS WITH b") { it.oC_ComparisonExpression() }
        "a ends  with b".testEquality("a ENDS WITH b") { it.oC_ComparisonExpression() }
        "a ends with  b".testEquality("a ENDS WITH b") { it.oC_ComparisonExpression() }
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
