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

import at.jku.faw.symspace.cypherrewriter.core.antlr.CypherBaseVisitor
import at.jku.faw.symspace.cypherrewriter.core.antlr.CypherParser
import at.jku.faw.symspace.cypherrewriter.core.cypher.*
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.tree.TerminalNode

class CypherRewritingVisitor : CypherBaseVisitor<AstNode>() {

    private fun add(node: AstInternalNode, childNodes: List<ParserRuleContext>) {
        childNodes.forEach { add(node, it) }
    }

    private fun add(node: AstNode, childNode: AstNode?): Boolean {
        childNode ?: return false

        if (node !is AstInternalNode) {
            throw ParseCancellationException("AstElement should be AstNode but is not")
        }

        when {
            isChildNodeTemporary(childNode) -> {
                childNode.asNode().elements.forEach {
                    node.elements.add(it)
                }
            }
            isEmptyGroup(childNode) -> {} //ignore
            isParentGroupAndChildGroupWithOneElement(node, childNode) -> {
                val soleElement = childNode.asNode().elements[0]
                node.elements.add(soleElement)
            }
            else -> node.elements.add(childNode)
        }

        return true
    }

    private fun isChildNodeTemporary(childNode: AstNode): Boolean {
        return childNode is AstInternalNode && childNode.type == AstType.TEMPORARY
    }

    private fun isEmptyGroup(childNode: AstNode): Boolean {
        if(childNode is AstInternalNode && childNode.type in setOf( AstType.GROUP, AstType.STRUCTURAL_GROUP)) {
            return childNode.elements.isEmpty()
        }
        return false
    }

    private fun isParentGroupAndChildGroupWithOneElement(node: AstNode, childNode: AstNode): Boolean {
        if (node !is AstInternalNode || childNode !is AstInternalNode) {
            return false
        }
        if(node.type != AstType.GROUP) {
            return false
        }
        if(childNode.type !in setOf(AstType.GROUP, AstType.STRUCTURAL_GROUP)) {
            return false
        }
        return childNode.elements.size == 1
    }

    private fun add(node: AstInternalNode, ctx: ParserRuleContext?): Boolean {
        ctx ?: return false
        return add(node, visit(ctx))
    }

    private fun exists(value: TerminalNode?): Boolean {
        return value != null
    }

    private fun exists(value: ParserRuleContext?): Boolean {
        return value != null
    }

    override fun aggregateResult(aggregate: AstNode?, nextResult: AstNode?): AstNode {
        val myAggregate = aggregate ?: AstInternalNode(AstType.TEMPORARY)

        if (nextResult != null) {
            add(myAggregate, nextResult)
        }

        return myAggregate
    }

    override fun visitOC_Cypher(ctx: CypherParser.OC_CypherContext): AstInternalNode {
        return AstInternalNode(AstType.QUERY).also { add(it, ctx.oC_Statement()) }
    }

    override fun visitOC_SinglePartQuery(ctx: CypherParser.OC_SinglePartQueryContext): AstInternalNode {
        val node = AstInternalNode(AstType.TEMPORARY)

        add(node, ctx.oC_ReadingClause())
        add(node, ctx.oC_UpdatingClause())
        add(node, ctx.oC_Return())

        return node
    }

    override fun visitOC_MultiPartQuery(ctx: CypherParser.OC_MultiPartQueryContext): AstInternalNode {
        val node = AstInternalNode(AstType.TEMPORARY)

        add(node, ctx.oC_MultiPartQueryGroup())
        add(node, ctx.oC_SinglePartQuery())

        return node
    }

    override fun visitOC_MultiPartQueryGroup(ctx: CypherParser.OC_MultiPartQueryGroupContext): AstInternalNode {
        val node = AstInternalNode(AstType.TEMPORARY)

        add(node, ctx.oC_ReadingClause())
        add(node, ctx.oC_UpdatingClause())
        add(node, ctx.oC_With())

        return node
    }

    override fun visitOC_Match(ctx: CypherParser.OC_MatchContext): AstInternalNode {
        val node = AstInternalNode(AstType.MATCH)
        if (exists(ctx.OPTIONAL())) {
            add(node, AstLeafNoValue(AstType.OPTIONAL))
        }
        add(node, ctx.oC_Pattern())
        add(node, ctx.oC_Where())
        return node
    }

    override fun visitOC_PatternPart(ctx: CypherParser.OC_PatternPartContext): AstInternalNode {
        return AstInternalNode(AstType.PATTERN).also {
            add(it, visitChildren(ctx))
        }
    }

    override fun visitOC_NodePattern(ctx: CypherParser.OC_NodePatternContext): AstNode {
        return AstInternalNode(AstType.NODE).also {
            add(it, visitChildren(ctx))
        }
    }

    override fun visitOC_LabelName(ctx: CypherParser.OC_LabelNameContext): AstLeafValue {
        return AstLeafValue(AstType.NODE_LABEL, ctx.oC_SchemaName().text)
    }

    override fun visitOC_Parameter(ctx: CypherParser.OC_ParameterContext): AstLeafValue {
        return AstLeafValue(AstType.PARAMETER, ctx.oC_SymbolicName().text)
    }

    override fun visitOC_PropertyKeyName(ctx: CypherParser.OC_PropertyKeyNameContext): AstLeafValue {
        return AstLeafValue(AstType.PROPERTY_KEY_NAME, ctx.oC_SchemaName().text)
    }

    override fun visitOC_MapLiteral(ctx: CypherParser.OC_MapLiteralContext): AstInternalNode {
        return AstInternalNode(AstType.PROPERTIES).also {
            for (i in 0 until ctx.oC_PropertyKeyName().size) {
                val node = AstInternalNode(AstType.PROPERTY)
                add(node, ctx.oC_PropertyKeyName(i))
                add(node, ctx.oC_Expression(i))
                add(it, node)
            }
        }
    }

    override fun visitOC_Return(ctx: CypherParser.OC_ReturnContext): AstInternalNode {
        val node = AstInternalNode(AstType.RETURN)
        add(node, ctx.oC_ProjectionBody())
        return node
    }

    override fun visitOC_ProjectionBody(ctx: CypherParser.OC_ProjectionBodyContext): AstInternalNode {
        val node = AstInternalNode(AstType.TEMPORARY)
        if (ctx.DISTINCT() != null) {
            add(node, AstLeafNoValue(AstType.DISTINCT))
        }

        add(node, visitChildren(ctx))

        return node
    }

    override fun visitOC_ProjectionItems(ctx: CypherParser.OC_ProjectionItemsContext): AstInternalNode {
        val node = AstInternalNode(AstType.TEMPORARY)
        if (ctx.text.startsWith("*")) {
            add(node, AstLeafNoValue(AstType.ASTERISK))
        }
        add(node, visitChildren(ctx))
        return node
    }

    override fun visitOC_Order(ctx: CypherParser.OC_OrderContext?): AstInternalNode {
        return AstInternalNode(AstType.ORDER_BY).also {
            add(it, visitChildren(ctx))
        }
    }

    override fun visitOC_SortItem(ctx: CypherParser.OC_SortItemContext): AstInternalNode {
        return AstInternalNode(AstType.SORT_ITEM).also {
            add(it, ctx.oC_Expression())
            if (exists(ctx.ASC()) || exists(ctx.ASCENDING())) {
                add(it, AstLeafNoValue(AstType.ASC))
            } else if (exists(ctx.DESC()) || exists(ctx.DESCENDING())) {
                add(it, AstLeafNoValue(AstType.DESC))
            }
        }
    }


    override fun visitOC_Skip(ctx: CypherParser.OC_SkipContext): AstInternalNode {
        return AstInternalNode(AstType.SKIP).also { add(it, visitChildren(ctx)) }
    }

    override fun visitOC_Limit(ctx: CypherParser.OC_LimitContext): AstInternalNode {
        return AstInternalNode(AstType.LIMIT).also { add(it, visitChildren(ctx)) }
    }

    override fun visitOC_ProjectionItem(ctx: CypherParser.OC_ProjectionItemContext): AstInternalNode {
        val node = when (exists(ctx.AS())) {
            true -> AstInternalNode(AstType.AS)
            false -> AstInternalNode(AstType.TEMPORARY)
        }

        add(node, visitChildren(ctx))
        return node
    }

    override fun visitOC_Variable(ctx: CypherParser.OC_VariableContext): AstLeafValue {
        return AstLeafValue(AstType.VARIABLE, ctx.text)
    }

    override fun visitOC_OrExpression(ctx: CypherParser.OC_OrExpressionContext): AstInternalNode {
        return when (ctx.OR().size > 0) {
            true -> AstInternalNode(AstType.OR)
            false -> AstInternalNode(AstType.TEMPORARY)
        }.also {
            add(it, visitChildren(ctx))
        }
    }

    override fun visitOC_XorExpression(ctx: CypherParser.OC_XorExpressionContext): AstInternalNode {
        return when (ctx.XOR().size > 0) {
            true -> AstInternalNode(AstType.XOR)
            false -> AstInternalNode(AstType.TEMPORARY)
        }.also {
            add(it, visitChildren(ctx))
        }
    }

    override fun visitOC_AndExpression(ctx: CypherParser.OC_AndExpressionContext): AstInternalNode {
        return when (ctx.AND().size > 0) {
            true -> AstInternalNode(AstType.AND)
            false -> AstInternalNode(AstType.TEMPORARY)
        }.also {
            ctx.children.forEach {childCtx ->
                val childNode = visit(childCtx)
                if (childNode is AstInternalNode && childNode.type == AstType.STRUCTURAL_GROUP && childNode.elements.size == 1) {
                    add(it, childNode)
                } else if (childNode is AstInternalNode && childNode.type == AstType.STRUCTURAL_GROUP && childNode.elements.size == 0) {
                    // ignore
                } else {
                    val group = AstInternalNode(AstType.STRUCTURAL_GROUP)
                    add(group, childNode)
                    add(it, group)
                }
            }
        }
    }

    override fun visitOC_NotExpression(ctx: CypherParser.OC_NotExpressionContext): AstInternalNode {
        val returnNode = AstInternalNode(AstType.TEMPORARY)
        var node: AstInternalNode = returnNode

        for (i in 1..ctx.NOT().size) {
            val newNode = AstInternalNode(AstType.NOT)
            add(node, newNode)
            node = newNode
        }

        add(node, visitChildren(ctx))
        return returnNode
    }

    /*override fun visitOC_ComparisonExpression(ctx: CypherParser.OC_ComparisonExpressionContext): AstNode {
        //TODO complete
        return AstValue(AstType.EXPRESSION, ctx.text)
    }*/

    override fun visitOC_RelationshipPattern(ctx: CypherParser.OC_RelationshipPatternContext): AstInternalNode {
        val node = when {
            exists(ctx.oC_LeftArrowHead()) && !exists(ctx.oC_RightArrowHead()) -> AstType.RELATION_LEFT
            !exists(ctx.oC_LeftArrowHead()) && exists(ctx.oC_RightArrowHead()) -> AstType.RELATION_RIGHT
            else -> AstType.RELATION_BOTH
        }.let { AstInternalNode(it) }

        add(node, visitChildren(ctx))
        return node
    }

    override fun visitOC_RelTypeName(ctx: CypherParser.OC_RelTypeNameContext): AstLeafValue {
        return AstLeafValue(AstType.RELATION_LABEL, ctx.text)
    }

    override fun visitOC_RangeLiteral(ctx: CypherParser.OC_RangeLiteralContext): AstNode {
        return when (ctx.oC_IntegerLiteral().size) {
            0 -> AstLeafNoValue(AstType.RANGE_ONE_OR_MORE)
            1 -> processSingleRangeDefinition(ctx)
            2 -> processFullRangeDefinition(ctx)
            else -> throw ParseCancellationException("Invalid range literal")
        }

    }

    private fun processSingleRangeDefinition(ctx: CypherParser.OC_RangeLiteralContext): AstLeafValue {
        var type = AstType.RANGE_EXACTLY
        var dotsSeen = false
        for (child in ctx.children) {
            if (child is TerminalNode && child.text == "..") {
                dotsSeen = true
                type = AstType.RANGE_FROM
            }
            if (dotsSeen && child is CypherParser.OC_IntegerLiteralContext) {
                type = AstType.RANGE_TO
            }
        }

        return AstLeafValue(type, ctx.oC_IntegerLiteral(0).text.toInt())
    }

    private fun processFullRangeDefinition(ctx: CypherParser.OC_RangeLiteralContext): AstInternalNode {
        val tmp = AstInternalNode(AstType.TEMPORARY)
        AstLeafValue(AstType.RANGE_FROM, ctx.oC_IntegerLiteral(0).text.toInt()).also { add(tmp, it) }
        AstLeafValue(AstType.RANGE_TO, ctx.oC_IntegerLiteral(1).text.toInt()).also { add(tmp, it) }
        return tmp
    }

    override fun visitOC_DoubleLiteral(ctx: CypherParser.OC_DoubleLiteralContext): AstNode {
        return AstLeafValue(AstType.DOUBLE, ctx.text.toDouble())
    }

    override fun visitOC_IntegerLiteral(ctx: CypherParser.OC_IntegerLiteralContext): AstNode {
        return AstLeafValue(AstType.INTEGER, ctx.text.toInt())
    }

    override fun visitOC_Literal(ctx: CypherParser.OC_LiteralContext): AstNode {
        return when {
            exists(ctx.StringLiteral()) -> AstLeafValue(AstType.STRING, ctx.text)
            exists(ctx.NULL()) -> AstLeafNoValue(AstType.NULL)
            else -> visitChildren(ctx)
        }
    }

    override fun visitOC_FunctionInvocation(ctx: CypherParser.OC_FunctionInvocationContext): AstNode {
        val node = AstInternalNode(AstType.FUNCTION_INVOCATION)
        add(node, ctx.oC_FunctionName())
        ctx.DISTINCT()?.let { add(node, visit(ctx.DISTINCT())) }
        ctx.oC_Expression().forEach {
            val arg = AstInternalNode(AstType.ARGUMENT)
            add(arg, visit(it))
            add(node, arg)
        }
        return node
    }

    override fun visitOC_FunctionName(ctx: CypherParser.OC_FunctionNameContext): AstNode {
        return AstLeafValue(AstType.FUNCTION_NAME, ctx.text)
    }

    override fun visitOC_ParenthesizedExpression(ctx: CypherParser.OC_ParenthesizedExpressionContext?): AstNode {
        val node = AstInternalNode(AstType.GROUP)
        add(node, visitChildren(ctx))
        return node
    }

    override fun visitOC_Where(ctx: CypherParser.OC_WhereContext?): AstNode {
        val node = AstInternalNode(AstType.WHERE)
        add(node, visitChildren(ctx))
        return node
    }

    override fun visitOC_PropertyOrLabelsExpression(ctx: CypherParser.OC_PropertyOrLabelsExpressionContext?): AstNode {
        val node = AstInternalNode(AstType.PROPERTY_DOT_ACCESS)
        add(node, visitChildren(ctx))
        return node
    }

    override fun visitOC_PartialComparisonExpression(ctx: CypherParser.OC_PartialComparisonExpressionContext): AstNode {
        val node = AstInternalNode(AstType.TEMPORARY)
        val valNode = when {
            ctx.text.startsWith("=") -> AstLeafValue(AstType.COMPARISON, "=")
            ctx.text.startsWith("<>") -> AstLeafValue(AstType.COMPARISON, "<>")
            ctx.text.startsWith("<") -> AstLeafValue(AstType.COMPARISON, "<")
            ctx.text.startsWith(">") -> AstLeafValue(AstType.COMPARISON, ">")
            ctx.text.startsWith(">=") -> AstLeafValue(AstType.COMPARISON, ">=")
            ctx.text.startsWith("<=") -> AstLeafValue(AstType.COMPARISON, "<=")
            else -> throw ParseCancellationException("Unrecognized comparison operator in \"${ctx.text}\"")
        }
        add(node, valNode)
        add(node, visitChildren(ctx))
        return node
    }


}
