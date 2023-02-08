package at.jku.faw.symspace.cypherrewriter.core.cypher.parser

import at.jku.faw.symspace.cypherrewriter.core.antlr.CypherBaseVisitor
import at.jku.faw.symspace.cypherrewriter.core.antlr.CypherParser
import at.jku.faw.symspace.cypherrewriter.core.cypher.*
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.tree.TerminalNode

class NewCypherVisitor : CypherBaseVisitor<AstElement>() {

    private fun add(node: AstNode, childNodes: List<ParserRuleContext>) {
        childNodes.forEach { add(node, it) }
    }

    private fun add(node: AstElement, childNode: AstElement?): Boolean {
        childNode ?: return false

        if (node !is AstNode) {
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

    private fun isChildNodeTemporary(childNode: AstElement): Boolean {
        return childNode is AstNode && childNode.type == AstType.TEMPORARY
    }

    private fun isEmptyGroup(childNode: AstElement): Boolean {
        if(childNode is AstNode && childNode.type in setOf( AstType.GROUP, AstType.STRUCTURAL_GROUP)) {
            return childNode.elements.isEmpty()
        }
        return false
    }

    private fun isParentGroupAndChildGroupWithOneElement(node: AstElement, childNode: AstElement): Boolean {
        if (node !is AstNode || childNode !is AstNode) {
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

    private fun add(node: AstNode, ctx: ParserRuleContext?): Boolean {
        ctx ?: return false
        return add(node, visit(ctx))
    }

    private fun exists(value: TerminalNode?): Boolean {
        return value != null
    }

    private fun exists(value: ParserRuleContext?): Boolean {
        return value != null
    }

    override fun aggregateResult(aggregate: AstElement?, nextResult: AstElement?): AstElement {
        val myAggregate = aggregate ?: AstNode(AstType.TEMPORARY)

        if (nextResult != null) {
            add(myAggregate, nextResult)
        }

        return myAggregate
    }

    override fun visitOC_Cypher(ctx: CypherParser.OC_CypherContext): AstNode {
        return AstNode(AstType.QUERY).also { add(it, ctx.oC_Statement()) }
    }

    override fun visitOC_SinglePartQuery(ctx: CypherParser.OC_SinglePartQueryContext): AstNode {
        val node = AstNode(AstType.TEMPORARY)

        add(node, ctx.oC_ReadingClause())
        add(node, ctx.oC_UpdatingClause())
        add(node, ctx.oC_Return())

        return node
    }

    override fun visitOC_MultiPartQuery(ctx: CypherParser.OC_MultiPartQueryContext): AstNode {
        val node = AstNode(AstType.TEMPORARY)

        add(node, ctx.oC_MultiPartQueryGroup())
        add(node, ctx.oC_SinglePartQuery())

        return node
    }

    override fun visitOC_MultiPartQueryGroup(ctx: CypherParser.OC_MultiPartQueryGroupContext): AstNode {
        val node = AstNode(AstType.TEMPORARY)

        add(node, ctx.oC_ReadingClause())
        add(node, ctx.oC_UpdatingClause())
        add(node, ctx.oC_With())

        return node
    }

    override fun visitOC_Match(ctx: CypherParser.OC_MatchContext): AstNode {
        val node = AstNode(AstType.MATCH)
        if (exists(ctx.OPTIONAL())) {
            add(node, AstTerminal(AstType.OPTIONAL))
        }
        add(node, ctx.oC_Pattern())
        add(node, ctx.oC_Where())
        return node
    }

    override fun visitOC_PatternPart(ctx: CypherParser.OC_PatternPartContext): AstNode {
        return AstNode(AstType.PATTERN).also {
            add(it, visitChildren(ctx))
        }
    }

    override fun visitOC_NodePattern(ctx: CypherParser.OC_NodePatternContext): AstElement {
        return AstNode(AstType.NODE).also {
            add(it, visitChildren(ctx))
        }
    }

    override fun visitOC_LabelName(ctx: CypherParser.OC_LabelNameContext): AstValue {
        return AstValue(AstType.NODE_LABEL, ctx.oC_SchemaName().text)
    }

    override fun visitOC_Parameter(ctx: CypherParser.OC_ParameterContext): AstValue {
        return AstValue(AstType.PARAMETER, ctx.oC_SymbolicName().text)
    }

    override fun visitOC_PropertyKeyName(ctx: CypherParser.OC_PropertyKeyNameContext): AstValue {
        return AstValue(AstType.PROPERTY_KEY_NAME, ctx.oC_SchemaName().text)
    }

    override fun visitOC_MapLiteral(ctx: CypherParser.OC_MapLiteralContext): AstNode {
        return AstNode(AstType.PROPERTIES).also {
            for (i in 0 until ctx.oC_PropertyKeyName().size) {
                val node = AstNode(AstType.PROPERTY)
                add(node, ctx.oC_PropertyKeyName(i))
                add(node, ctx.oC_Expression(i))
                add(it, node)
            }
        }
    }

    override fun visitOC_Return(ctx: CypherParser.OC_ReturnContext): AstNode {
        val node = AstNode(AstType.RETURN)
        add(node, ctx.oC_ProjectionBody())
        return node
    }

    override fun visitOC_ProjectionBody(ctx: CypherParser.OC_ProjectionBodyContext): AstNode {
        val node = AstNode(AstType.TEMPORARY)
        if (ctx.DISTINCT() != null) {
            add(node, AstTerminal(AstType.DISTINCT))
        }

        add(node, visitChildren(ctx))

        return node
    }

    override fun visitOC_ProjectionItems(ctx: CypherParser.OC_ProjectionItemsContext): AstNode {
        val node = AstNode(AstType.TEMPORARY)
        if (ctx.text.startsWith("*")) {
            add(node, AstTerminal(AstType.ASTERISK))
        }
        add(node, visitChildren(ctx))
        return node
    }

    override fun visitOC_Order(ctx: CypherParser.OC_OrderContext?): AstNode {
        return AstNode(AstType.ORDER_BY).also {
            add(it, visitChildren(ctx))
        }
    }

    override fun visitOC_SortItem(ctx: CypherParser.OC_SortItemContext): AstNode {
        return AstNode(AstType.SORT_ITEM).also {
            add(it, ctx.oC_Expression())
            if (exists(ctx.ASC()) || exists(ctx.ASCENDING())) {
                add(it, AstTerminal(AstType.ASC))
            } else if (exists(ctx.DESC()) || exists(ctx.DESCENDING())) {
                add(it, AstTerminal(AstType.DESC))
            }
        }
    }


    override fun visitOC_Skip(ctx: CypherParser.OC_SkipContext): AstNode {
        return AstNode(AstType.SKIP).also { add(it, visitChildren(ctx)) }
    }

    override fun visitOC_Limit(ctx: CypherParser.OC_LimitContext): AstNode {
        return AstNode(AstType.LIMIT).also { add(it, visitChildren(ctx)) }
    }

    override fun visitOC_ProjectionItem(ctx: CypherParser.OC_ProjectionItemContext): AstNode {
        val node = when (exists(ctx.AS())) {
            true -> AstNode(AstType.AS)
            false -> AstNode(AstType.TEMPORARY)
        }

        add(node, visitChildren(ctx))
        return node
    }

    override fun visitOC_Variable(ctx: CypherParser.OC_VariableContext): AstValue {
        return AstValue(AstType.VARIABLE, ctx.text)
    }

    override fun visitOC_OrExpression(ctx: CypherParser.OC_OrExpressionContext): AstNode {
        return when (ctx.OR().size > 0) {
            true -> AstNode(AstType.OR)
            false -> AstNode(AstType.TEMPORARY)
        }.also {
            add(it, visitChildren(ctx))
        }
    }

    override fun visitOC_XorExpression(ctx: CypherParser.OC_XorExpressionContext): AstNode {
        return when (ctx.XOR().size > 0) {
            true -> AstNode(AstType.XOR)
            false -> AstNode(AstType.TEMPORARY)
        }.also {
            add(it, visitChildren(ctx))
        }
    }

    override fun visitOC_AndExpression(ctx: CypherParser.OC_AndExpressionContext): AstNode {
        return when (ctx.AND().size > 0) {
            true -> AstNode(AstType.AND)
            false -> AstNode(AstType.TEMPORARY)
        }.also {
            ctx.children.forEach {childCtx ->
                val childNode = visit(childCtx)
                if (childNode is AstNode && childNode.type == AstType.STRUCTURAL_GROUP && childNode.elements.size == 1) {
                    add(it, childNode)
                } else if (childNode is AstNode && childNode.type == AstType.STRUCTURAL_GROUP && childNode.elements.size == 0) {
                    // ignore
                } else {
                    val group = AstNode(AstType.STRUCTURAL_GROUP)
                    add(group, childNode)
                    add(it, group)
                }
            }
        }
    }

    override fun visitOC_NotExpression(ctx: CypherParser.OC_NotExpressionContext): AstNode {
        val returnNode = AstNode(AstType.TEMPORARY)
        var node: AstNode = returnNode

        for (i in 1..ctx.NOT().size) {
            val newNode = AstNode(AstType.NOT)
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

    override fun visitOC_RelationshipPattern(ctx: CypherParser.OC_RelationshipPatternContext): AstNode {
        val node = when {
            exists(ctx.oC_LeftArrowHead()) && !exists(ctx.oC_RightArrowHead()) -> AstType.RELATION_LEFT
            !exists(ctx.oC_LeftArrowHead()) && exists(ctx.oC_RightArrowHead()) -> AstType.RELATION_RIGHT
            else -> AstType.RELATION_BOTH
        }.let { AstNode(it) }

        add(node, visitChildren(ctx))
        return node
    }

    override fun visitOC_RelTypeName(ctx: CypherParser.OC_RelTypeNameContext): AstValue {
        return AstValue(AstType.RELATION_LABEL, ctx.text)
    }

    override fun visitOC_RangeLiteral(ctx: CypherParser.OC_RangeLiteralContext): AstElement {
        return when (ctx.oC_IntegerLiteral().size) {
            0 -> AstTerminal(AstType.RANGE_ONE_OR_MORE)
            1 -> processSingleRangeDefinition(ctx)
            2 -> processFullRangeDefinition(ctx)
            else -> throw ParseCancellationException("Invalid range literal")
        }

    }

    private fun processSingleRangeDefinition(ctx: CypherParser.OC_RangeLiteralContext): AstValue {
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

        return AstValue(type, ctx.oC_IntegerLiteral(0).text.toInt())
    }

    private fun processFullRangeDefinition(ctx: CypherParser.OC_RangeLiteralContext): AstNode {
        val tmp = AstNode(AstType.TEMPORARY)
        AstValue(AstType.RANGE_FROM, ctx.oC_IntegerLiteral(0).text.toInt()).also { add(tmp, it) }
        AstValue(AstType.RANGE_TO, ctx.oC_IntegerLiteral(1).text.toInt()).also { add(tmp, it) }
        return tmp
    }

    override fun visitOC_DoubleLiteral(ctx: CypherParser.OC_DoubleLiteralContext): AstElement {
        return AstValue(AstType.DOUBLE, ctx.text.toDouble())
    }

    override fun visitOC_IntegerLiteral(ctx: CypherParser.OC_IntegerLiteralContext): AstElement {
        return AstValue(AstType.INTEGER, ctx.text.toInt())
    }

    override fun visitOC_Literal(ctx: CypherParser.OC_LiteralContext): AstElement {
        return when {
            exists(ctx.StringLiteral()) -> AstValue(AstType.STRING, ctx.text)
            exists(ctx.NULL()) -> AstTerminal(AstType.NULL)
            else -> visitChildren(ctx)
        }
    }

    override fun visitOC_FunctionInvocation(ctx: CypherParser.OC_FunctionInvocationContext): AstElement {
        val node = AstNode(AstType.FUNCTION_INVOCATION)
        add(node, ctx.oC_FunctionName())
        ctx.DISTINCT()?.let { add(node, visit(ctx.DISTINCT())) }
        ctx.oC_Expression().forEach {
            val arg = AstNode(AstType.ARGUMENT)
            add(arg, visit(it))
            add(node, arg)
        }
        return node
    }

    override fun visitOC_FunctionName(ctx: CypherParser.OC_FunctionNameContext): AstElement {
        return AstValue(AstType.FUNCTION_NAME, ctx.text)
    }

    override fun visitOC_ParenthesizedExpression(ctx: CypherParser.OC_ParenthesizedExpressionContext?): AstElement {
        val node = AstNode(AstType.GROUP)
        add(node, visitChildren(ctx))
        return node
    }

    override fun visitOC_Where(ctx: CypherParser.OC_WhereContext?): AstElement {
        val node = AstNode(AstType.WHERE)
        add(node, visitChildren(ctx))
        return node
    }

    override fun visitOC_PropertyOrLabelsExpression(ctx: CypherParser.OC_PropertyOrLabelsExpressionContext?): AstElement {
        val node = AstNode(AstType.PROPERTY_DOT_ACCESS)
        add(node, visitChildren(ctx))
        return node
    }

    override fun visitOC_PartialComparisonExpression(ctx: CypherParser.OC_PartialComparisonExpressionContext): AstElement {
        val node = AstNode(AstType.TEMPORARY)
        val valNode = when {
            ctx.text.startsWith("=") -> AstValue(AstType.COMPARISON, "=")
            ctx.text.startsWith("<>") -> AstValue(AstType.COMPARISON, "<>")
            ctx.text.startsWith("<") -> AstValue(AstType.COMPARISON, "<")
            ctx.text.startsWith(">") -> AstValue(AstType.COMPARISON, ">")
            ctx.text.startsWith(">=") -> AstValue(AstType.COMPARISON, ">=")
            ctx.text.startsWith("<=") -> AstValue(AstType.COMPARISON, "<=")
            else -> throw ParseCancellationException("Unrecognized comparison operator in \"${ctx.text}\"")
        }
        add(node, valNode)
        add(node, visitChildren(ctx))
        return node
    }


}
