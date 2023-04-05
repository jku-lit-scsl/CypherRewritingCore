package at.jku.faw.symspace.cypherrewriter.core.cypher


class PermissionConfig(
    val policies: List<Policy>,
    val filterTemplates: List<FilterTemplate>
)

class Policy (
    val pattern: String,
    val patternMatchStrategy: Map<String, PatternMatchStrategy>,
    val rules: List<Rule>,
    val comment: String? = null
)

class Rule (
    val id: String,
    val variable: String,
    val conditions: List<Condition>,
    val filterId: String,
    val authorizationLevel: AuthorizationLevel,
    val comment: String? = null
)

abstract class Condition

class ConditionExpression (
    val variable: String,
    val filterType: FilterType,
    val returnType: ReturnType,
    val comment: String? = null
): Condition()

class ConditionCombination(
    val operation: ConditionBoolean,
    vararg val conditions: Condition
): Condition()

enum class ConditionBoolean {
    AND, OR, NOT
}

class FilterTemplate (
    val filterId: String,
    val authorizationLevel: AuthorizationLevel,
    val pattern: String,
    val arguments: List<ArgumentType>,
    val comment: String? = null
)

class PatternMatchStrategy (
    val labelStrategy: LabelMatchStrategy,
    val matchEmptyLabels: Boolean
)

enum class LabelMatchStrategy {
    EXACT,
    CONTAINS_ALL,
    CONTAINS_ANY
}

enum class AuthorizationLevel {
    PUBLIC_LEVEL,
    OWNER_LEVEL,
    AUTHORIZED_LEVEL
}

object ReturnTypeMetadata {
    val aggregationFunctions = listOf("sum", "avg")
    val anyReturnTypes = setOf(ReturnType.RETURNED_AS_VALUE, ReturnType.AGGREGATED)
}

enum class ArgumentType {
    USERNAME,
    RESOURCE_VARIABLE
}

enum class ReturnType {
    ANY,
    NO_RETURN,
    ANY_RETURN,
    AGGREGATED,
    RETURNED_AS_VALUE
}

enum class FilterType {
    ANY,
    NO_FILTER,
    FILTERED
}

