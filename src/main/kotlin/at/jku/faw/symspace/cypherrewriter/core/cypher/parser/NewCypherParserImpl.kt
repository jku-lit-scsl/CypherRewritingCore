package at.jku.faw.symspace.cypherrewriter.core.cypher.parser

import at.jku.faw.symspace.cypherrewriter.core.antlr.CypherLexer
import at.jku.faw.symspace.cypherrewriter.core.antlr.CypherParser
import at.jku.faw.symspace.cypherrewriter.core.cypher.AstElement
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class NewCypherParserImpl : NewCypherParser {
    override fun parse(input: String): AstElement {
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

    override fun parse(input: ParserRuleContext?): AstElement {
        return visitor.visit(input)
    }

    override fun parse(input: String, contextGetter: (CypherParser) -> ParserRuleContext): AstElement {
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
