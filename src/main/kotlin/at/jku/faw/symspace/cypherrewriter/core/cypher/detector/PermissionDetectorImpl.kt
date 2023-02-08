package at.jku.faw.symspace.cypherrewriter.core.cypher.detector

import at.jku.faw.symspace.cypherrewriter.core.cypher.*
import org.springframework.stereotype.Component

@Component
class PermissionDetectorImpl(val subDetectors: List<SubDetector>) : PermissionDetector {
    override fun process(node: AstNode): List<Detection> {
        val detections = subDetectors.flatMap { it.process(node) }
        val ctx = Context()
        extractInformation(ctx, node)
        detections.forEach { interpret(ctx, it) }
        return detections
    }

    private fun extractInformation(ctx: Context, node: AstElement) {
        when (node.type) {
            AstType.RETURN -> processReturn(ctx, node as AstNode)
            AstType.ASTERISK -> ctx.returnsAsterisk = true
            AstType.FUNCTION_INVOCATION -> processFunctionInvocation(ctx, node as AstNode)
            AstType.FUNCTION_NAME -> ctx.currentFunctionContext?.name = node.asValue().value.toString()
            AstType.VARIABLE -> processVariable(ctx, node.asValue())
            AstType.WHERE -> processWhere(ctx, node.asNode())
            else -> traverse(ctx, node)
        }
    }

    private fun processWhere(ctx: Context, node: AstNode) {
        val whereContext = WhereContext()

        ctx.currentWhereContext = whereContext
        traverse(ctx, node)
        ctx.currentWhereContext = null

        ctx.filteredVariables.addAll(whereContext.variables)
    }

    private fun traverse(ctx: Context, ast: AstElement) {
        if (ast is AstNode) {
            ast.elements.forEach {
                extractInformation(ctx, it)
            }
        }
    }

    private fun interpret(ctx: Context, detection: Detection) {
        val variableName = detection.protectedNode!!.elements.find { it.type == AstType.VARIABLE }?.asValue()?.value.toString()
        detection.returnType = interpretReturnType(ctx, variableName)

        val detectedFilter = interpretFilterType(ctx, variableName)
        if (assignDetectedFilter(detectedFilter, detection.filterType)) {
            detection.filterType = detectedFilter
        }
    }

    private fun assignDetectedFilter(detectedFilterType: FilterType, knownFilterType: FilterType?): Boolean {
        if (knownFilterType == null) {
            return true
        }
        if (knownFilterType == FilterType.FILTERED) {
            return false
        }
        return detectedFilterType != knownFilterType
    }

    private fun interpretFilterType(ctx: Context, variable: String?): FilterType {
        return if (ctx.filteredVariables.contains(variable)) {
            FilterType.FILTERED
        } else {
            FilterType.NO_FILTER
        }
    }

    private fun interpretReturnType(ctx: Context, variable: String?): ReturnType {
        return if (ctx.returnsAsterisk) {
            ReturnType.RETURNED_AS_VALUE
        } else if (variable == null) {
            ReturnType.NO_RETURN
        } else {
            val usages = ctx.returnedVariables[variable] ?: setOf()
            when {
                ReturnType.RETURNED_AS_VALUE in usages -> ReturnType.RETURNED_AS_VALUE
                ReturnType.AGGREGATED in usages -> ReturnType.AGGREGATED
                else -> ReturnType.NO_RETURN
            }
        }
    }

    private fun processFunctionInvocation(ctx: Context, astNode: AstNode) {
        val funCtx = FunctionContext()
        ctx.currentFunctionContext = funCtx
        traverse(ctx, astNode)
        ctx.currentFunctionContext = null
        for(variable in funCtx.variables) {
            val list = ctx.returnedVariables.computeIfAbsent(variable) { mutableSetOf() }
            if(funCtx.name in AggregationFunctions.names) {
                list.add(ReturnType.AGGREGATED)
            } else {
                list.add(ReturnType.RETURNED_AS_VALUE)
            }
        }

    }

    private fun processVariable(ctx: Context, astValue: AstValue) {
        val varName = astValue.value.toString()
        ctx.currentFunctionContext?.variables?.add(varName)
        ctx.currentWhereContext?.variables?.add(varName)
        if (ctx.returnNode != null && ctx.currentFunctionContext == null) {
            ctx.returnedVariables.computeIfAbsent(varName) { mutableSetOf() }.add(ReturnType.RETURNED_AS_VALUE)
        }
    }

    private fun processReturn(ctx: Context, astNode: AstNode) {
        ctx.returnNode = astNode
        traverse(ctx, astNode)
    }

    private class Context {
        var returnsAsterisk = false
        var returnNode: AstNode? = null
        val returnedVariables: MutableMap<String, MutableSet<ReturnType>> = mutableMapOf()
        val filteredVariables: MutableSet<String> = mutableSetOf()
        var currentFunctionContext: FunctionContext? = null
        var currentWhereContext: WhereContext? = null
    }

    private class FunctionContext {
        var name: String? = null
        var variables: MutableSet<String> = mutableSetOf()
    }

    private class WhereContext {
        var variables: MutableSet<String> = mutableSetOf()
    }
}
