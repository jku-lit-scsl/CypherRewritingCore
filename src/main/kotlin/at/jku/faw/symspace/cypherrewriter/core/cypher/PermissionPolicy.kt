package at.jku.faw.symspace.cypherrewriter.core.cypher


class PermissionConfig(
    val policies: List<Policy>,
    val filters: List<Filter>
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

class Condition (
    val variable: String,
    val filterType: FilterType,
    val returnType: ReturnType,
    val comment: String? = null
)

class Filter (
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


@Deprecated("Old policy model", ReplaceWith("PermissionConfig"))
interface PermissionPolicy {
    val rules: List<PolicyRule>
}

@Deprecated("Old policy model", ReplaceWith("Policy"))
class PolicyRule(
    val id: String,
    val description: String,
    val ressource: AstNode,
    val levels: AuthorizationLevelHolder,
    val enforcementFilter: String,
    val arguments: List<ArgumentType>,
    val subjectNodeExtractor: (AstNode) -> AstNode = {node -> node}
) {
    val subjectNode: AstNode get() {return subjectNodeExtractor(ressource)}
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

@Deprecated("Old policy model")
class AuthorizationLevelHolder(private val default: AuthorizationLevel = AuthorizationLevel.OWNER_LEVEL) {
    private val bothSpecified = mutableMapOf<Pair<ReturnType, FilterType>, AuthorizationLevel>()
    private val returnSpecified = mutableMapOf<ReturnType, AuthorizationLevel>()
    private val filterSpecified = mutableMapOf<FilterType, AuthorizationLevel>()

    fun set(returnType: ReturnType, filterType: FilterType, authorizationLevel: AuthorizationLevel): AuthorizationLevelHolder {
        val pair = returnType to filterType
        bothSpecified[pair] = authorizationLevel
        return this
    }

    fun set(returnType: ReturnType, authorizationLevel: AuthorizationLevel): AuthorizationLevelHolder {
        returnSpecified[returnType] = authorizationLevel
        return this
    }

    fun set(filterType: FilterType, authorizationLevel: AuthorizationLevel): AuthorizationLevelHolder {
        filterSpecified[filterType] = authorizationLevel
        return this
    }

    fun get(returnType: ReturnType?, filterType: FilterType?): AuthorizationLevel {
        bothSpecified[returnType to filterType]?.let { return it }
        returnSpecified[returnType]?.let { return it }
        filterSpecified[filterType]?.let { return it }
        return default
    }
}
