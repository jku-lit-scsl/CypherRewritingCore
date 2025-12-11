package at.jku.faw.symspace.cypherrewriter.core

import at.jku.faw.symspace.cypherrewriter.core.antlr.CypherParser
import at.jku.faw.symspace.cypherrewriter.core.cypher.PermissionConfig
import at.jku.faw.symspace.cypherrewriter.core.cypher.parser.NewCypherParser
import at.jku.faw.symspace.cypherrewriter.core.cypher.parser.NewCypherParserImpl
import at.jku.faw.symspace.cypherrewriter.core.cypher.renderer.AstVisualizer
import at.jku.faw.symspace.cypherrewriter.core.cypher.renderer.AstVisualizerImpl
import org.antlr.v4.runtime.ParserRuleContext
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ContextConfiguration(classes = [NewCypherParserImpl::class, AstVisualizerImpl::class])
class AstVisualizerTest(
    @Autowired private val parser: NewCypherParser,
    @Autowired private val visualizer: AstVisualizer
) {

    @Mock
    lateinit var permissionConfig: PermissionConfig

    @Test
    fun testVisualization() {
        "MATCH (p:Professor) RETURN p".printAst{ it.oC_Cypher() }
        "MATCH (p), (p:Professor) RETURN p".printAst{ it.oC_Cypher() }
        "MATCH (p) WHERE p:Professor RETURN p".printAst{ it.oC_Cypher() }
    }

    @Test
    fun visualizeFilterInMatch() {
        "MATCH (p:Professor {name: \"Josef\"}) RETURN p".printAst{ it.oC_Cypher() }
        "MATCH (p {}) RETURN p".printAst{ it.oC_Cypher() }
    }

    @Test
    fun visualizeFilterInWhere() {
        "MATCH (p:Professor) WHERE p.name = \"Josef\" RETURN p".printAst{ it.oC_Cypher() }
        "MATCH (p) WHERE p.name RETURN p".printAst{ it.oC_Cypher() }
        "MATCH (p) WHERE p RETURN p".printAst{ it.oC_Cypher() }
        "MATCH (p) WHERE trim(p.name) = \"Josef\" RETURN p".printAst{ it.oC_Cypher() }
        //"MATCH (p) WHERE (p)--({name: \"test\"}) RETURN p".printAst{ it.oC_Cypher() }
    }

    @Test
    fun visualizeCustomQuery() {
        "MATCH (p:Professor)-[:READS]->(l)--(s:Student{study: \"Computer Science\"}) WHERE l:Lecture AND p.name = \"Josef\" RETURN s".printAst { it.oC_Cypher() }
    }

    private fun String.printAst(input: String = this, function: (CypherParser) -> ParserRuleContext) {
        val context = function(parser.getParser(input))
        val ast = parser.parse(context)
        val output = visualizer.render(ast)
        println(output)
    }


}


