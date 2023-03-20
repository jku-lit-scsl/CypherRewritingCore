package at.jku.faw.symspace.cypherrewriter.core.cypher.parser

import at.jku.faw.symspace.cypherrewriter.core.antlr.CypherParser
import at.jku.faw.symspace.cypherrewriter.core.cypher.AstNode
import org.antlr.v4.runtime.ParserRuleContext

interface NewCypherParser {
    fun parse(input: String): AstNode
    fun getParser(input: String): CypherParser
    fun parse(input: ParserRuleContext?): AstNode
    fun parse(input: String, contextGetter: (CypherParser) -> ParserRuleContext): AstNode
}
