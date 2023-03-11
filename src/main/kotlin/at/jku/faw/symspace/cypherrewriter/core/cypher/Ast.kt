package at.jku.faw.symspace.cypherrewriter.core.cypher

sealed class AstElement(val type: AstType) {


    abstract fun copy(overrideType: AstType = this.type): AstElement
    fun asNode(): AstNode {
        return this as AstNode
    }

    fun asValue(): AstValue {
        return this as AstValue
    }
}

class AstNode(type: AstType) : AstElement(type) {
    val elements: MutableList<AstElement> = mutableListOf()

    override fun copy(overrideType: AstType): AstNode {
        return AstNode(overrideType).also { copy ->
            this.elements.map { it.copy() }.forEach { copy.elements.add(it) }
        }
    }
}

class AstValue(type: AstType, val value: Any) : AstElement(type) {
    override fun copy(overrideType: AstType): AstValue {
        return AstValue(overrideType, value)
    }
}

class AstTerminal(type: AstType) : AstElement(type) {
    override fun copy(overrideType: AstType): AstTerminal {
        return AstTerminal(overrideType)
    }
}

enum class AstType(val value: String? = null) {
    RETURN("RETURN"),
    WHERE("WHERE"),
    PATTERN("pattern"),
    OPTIONAL("OPTIONAL"),
    MATCH("MATCH"),
    DISTINCT("DISTINCT"),
    TEMPORARY,
    ORDER_BY("ORDER BY"),
    SKIP("SKIP"),
    LIMIT("LIMIT"),
    ASTERISK("*"),
    AS("AS"),
    VARIABLE,
    OR("OR"),
    XOR("XOR"),
    AND("AND"),
    NOT("NOT"),
    GROUP,
    STRUCTURAL_GROUP,
    EXPRESSION,
    SORT_ITEM,
    ASC("ASC"),
    DESC("DESC"),
    QUERY,
    NODE_LABEL,
    PARAMETER,
    PROPERTY_KEY_NAME,
    PROPERTIES,
    PROPERTY,
    PROPERTY_DOT_ACCESS,
    RELATION_BOTH,
    RELATION_LEFT,
    RELATION_RIGHT,
    RELATION_LABEL,
    INTEGER,
    DOUBLE,
    STRING,
    NODE,
    RANGE_EXACTLY,
    RANGE_ONE_OR_MORE("*"),
    RANGE_FROM,
    RANGE_TO,
    NULL("NULL"),
    FUNCTION_INVOCATION,
    FUNCTION_NAME,
    ARGUMENT,
    COMPARISON,
}

object AstMetadata {
    val SET_OF_VARIABLE_STORABLE_AST_TYPES = setOf(
        AstType.NODE,
        AstType.RELATION_BOTH,
        AstType.RELATION_RIGHT,
        AstType.RELATION_LEFT,
        AstType.PATTERN
    )
}
