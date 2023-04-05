package at.jku.faw.symspace.cypherrewriter.core.cypher

class PermissionConfigBuilder {

    private val policyBuilders: MutableList<PolicyBuilder> = mutableListOf()
    private val filterTemplates: MutableList<FilterTemplate> = mutableListOf()

    fun policy(comment: String? = null): PolicyBuilder {
        val policyBuilder = PolicyBuilder(this, comment)
        policyBuilders.add(policyBuilder)
        return policyBuilder
    }

    fun build(): PermissionConfig {
        val policies = policyBuilders.map { it.build() }

        return PermissionConfig(policies, filterTemplates)
    }

    fun filter(
        filterId: String,
        authorizationLevel: AuthorizationLevel,
        pattern: String,
        argumentTypes: List<ArgumentType>,
        comment: String? = null
    ): PermissionConfigBuilder {
        val filterTemplate = FilterTemplate(filterId, authorizationLevel, pattern, argumentTypes.toList(), comment)
        filterTemplates.add(filterTemplate)
        return this
    }

    fun condition(variable: String, filterType: FilterType, returnType: ReturnType, comment: String?): Condition {
        return ConditionExpression(variable, filterType, returnType, comment)
    }

    fun and(vararg conditions: Condition): Condition {
        return ConditionCombination(ConditionBoolean.AND, *conditions)
    }

    fun or(vararg conditions: Condition): Condition {
        return ConditionCombination(ConditionBoolean.OR, *conditions)
    }

    fun not(vararg conditions: Condition): Condition {
        return ConditionCombination(ConditionBoolean.NOT, *conditions)
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
        id: String,
        variable: String,
        filterId: String,
        authorizationLevel: AuthorizationLevel,
        comment: String? = null
    ): RuleBuilder {
        return RuleBuilder(this, id, variable, filterId, authorizationLevel, comment).also { ruleBuilders.add(it) }
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
    private val id: String,
    private val variable: String,
    private val filterId: String,
    private val authorizationLevel: AuthorizationLevel,
    private val comment: String?
) {
    private val conditions: MutableList<Condition> = mutableListOf()
    fun build(): Rule {
        return Rule(id, variable, conditions, filterId, authorizationLevel, comment)
    }

    fun on(vararg condition: Condition): PolicyBuilder {
        conditions.addAll(condition)
        return parent
    }

}

