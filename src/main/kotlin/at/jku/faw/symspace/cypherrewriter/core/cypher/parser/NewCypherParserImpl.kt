/*
 * Cypher Rewriting System
 * Copyright (C) 2023 Daniel Hofer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.jku.faw.symspace.cypherrewriter.core.cypher.parser

import at.jku.faw.symspace.cypherrewriter.core.antlr.CypherLexer
import at.jku.faw.symspace.cypherrewriter.core.antlr.CypherParser
import at.jku.faw.symspace.cypherrewriter.core.cypher.AstNode
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class NewCypherParserImpl : NewCypherParser {
    override fun parse(input: String): AstNode {
        return try {
            parse(getParser(input).oC_Cypher())
        } catch (e: Exception) {
            logger.error(String.format("Could not parse input query \"%s\"", input), e)
            throw e
        }
    }

    override fun getParser(input: String): CypherParser {
        val lexer = CypherLexer(CharStreams.fromString(input))
        lexer.addErrorListener(antlrErrorListener)
        val antlrParser = CypherParser(CommonTokenStream(lexer))
        antlrParser.addErrorListener(antlrErrorListener)
        return antlrParser
    }

    override fun parse(input: ParserRuleContext?): AstNode {
        return visitor.visit(input)
    }

    override fun parse(input: String, contextGetter: (CypherParser) -> ParserRuleContext): AstNode {
        return parse(contextGetter(getParser(input)))
    }

    companion object {
        private val visitor = NewCypherVisitor()
        private val logger = LoggerFactory.getLogger(NewCypherParserImpl::class.java)
        private val antlrErrorListener: ANTLRErrorListener = object : ANTLRErrorListener {
            override fun syntaxError(
                recognizer: Recognizer<*, *>?,
                offendingSymbol: Any,
                line: Int,
                charPositionInLine: Int,
                msg: String,
                e: RecognitionException
            ) {
                throw InvalidSyntaxException(msg)
            }

            override fun reportAmbiguity(
                recognizer: Parser,
                dfa: DFA,
                startIndex: Int,
                stopIndex: Int,
                exact: Boolean,
                ambigAlts: BitSet,
                configs: ATNConfigSet
            ) {
                throw IllegalArgumentException()
            }

            override fun reportAttemptingFullContext(
                recognizer: Parser,
                dfa: DFA,
                startIndex: Int,
                stopIndex: Int,
                conflictingAlts: BitSet,
                configs: ATNConfigSet
            ) {
                throw IllegalArgumentException()
            }

            override fun reportContextSensitivity(
                recognizer: Parser,
                dfa: DFA,
                startIndex: Int,
                stopIndex: Int,
                prediction: Int,
                configs: ATNConfigSet
            ) {
                throw IllegalArgumentException()
            }
        }
    }
}
