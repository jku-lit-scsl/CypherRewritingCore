package at.jku.faw.symspace.cypherrewriter.core.cypher

class PermissionConfigBuilder {

    val policyBuilders: MutableList<PolicyBuilder> = mutableListOf()
    val filters: MutableList<Filter> = mutableListOf()

    fun policy(comment: String? = null): PolicyBuilder {
        val policyBuilder = PolicyBuilder(this, comment)
        policyBuilders.add(policyBuilder)
        return policyBuilder
    }

    fun build(): PermissionConfig {
        val policies = policyBuilders.map { it.build() }

        return PermissionConfig(policies, listOf())
    }

    fun filter(
        filterId: String,
        authorizationLevel: AuthorizationLevel,
        pattern: String,
        argumentTypes: List<ArgumentType>,
        comment: String? = null
    ): PermissionConfigBuilder {
        val filter = Filter(filterId, authorizationLevel, pattern, argumentTypes.toList(), comment)
        filters.add(filter)
        return this
    }

}

class PolicyBuilder(private val parent: PermissionConfigBuilder, private val comment: String?) {
    private val ruleBuilders: MutableList<RuleBuilder> = mutableListOf()
    private val patternMatchStrategies: MutableMap<String, PatternMatchStrategy> = mutableMapOf()
    private lateinit var pattern: String
    fun pattern(pattern: String): PolicyBuilder {
        this.pattern = pattern
        return this
    }

    fun varMatching(
        variable: String,
        labelMatchStrategy: LabelMatchStrategy,
        matchEmptyLabels: Boolean
    ): PolicyBuilder {
        patternMatchStrategies[variable] = PatternMatchStrategy(labelMatchStrategy, matchEmptyLabels)
        return this
    }

    fun rule(
        variable: String,
        filterId: String,
        authorizationLevel: AuthorizationLevel,
        comment: String? = null
    ): RuleBuilder {
        return RuleBuilder(this, variable, filterId, authorizationLevel, comment).also { ruleBuilders.add(it) }
    }

    fun endPattern(): PermissionConfigBuilder {
        return parent
    }

    fun build(): Policy {
        return Policy(pattern, patternMatchStrategies, ruleBuilders.map { it.build() }, comment)
    }


}

class RuleBuilder(
    private val parent: PolicyBuilder,
    private val variable: String,
    private val filterId: String,
    private val authorizationLevel: AuthorizationLevel,
    private val comment: String?
) {
    private val conditions: MutableList<Condition> = mutableListOf()
    fun condition(
        variable: String,
        filterType: FilterType,
        returnType: ReturnType,
        comment: String? = null
    ): RuleBuilder {
        val condition = Condition(variable, filterType, returnType, comment)
        conditions.add(condition)
        return this
    }

    fun endRule(): PolicyBuilder {
        return parent
    }

    fun build(): Rule {
        return Rule(variable, conditions, filterId, authorizationLevel, comment)
    }

}

