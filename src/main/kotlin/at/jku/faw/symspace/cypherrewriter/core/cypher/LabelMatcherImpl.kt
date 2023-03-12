package at.jku.faw.symspace.cypherrewriter.core.cypher

import org.springframework.stereotype.Service

@Service
class LabelMatcherImpl : LabelMatcher {
    override fun buildLabelMatchMap(patternMatchStrategy: Map<String, PatternMatchStrategy>): MutableMap<String, (Set<String>, Set<String>) -> Boolean> {
        return patternMatchStrategy.map {
            when (it.value.labelStrategy) {
                LabelMatchStrategy.CONTAINS_ANY -> it.key to { policyLabels: Set<String>, queryLabels: Set<String> ->
                    matchLabelsQueryContainsAnyPolicy(
                        policyLabels, queryLabels, it.value.matchEmptyLabels
                    )
                }

                LabelMatchStrategy.CONTAINS_ALL -> it.key to { policyLabels: Set<String>, queryLabels: Set<String> ->
                    matchLabelsQueryContainsAllPolicy(
                        policyLabels, queryLabels, it.value.matchEmptyLabels
                    )
                }

                LabelMatchStrategy.EXACT -> it.key to { policyLabels: Set<String>, queryLabels: Set<String> ->
                    matchLabelsExactly(
                        policyLabels, queryLabels, it.value.matchEmptyLabels
                    )
                }
            }
        }.toMap().toMutableMap()
    }

    override fun anyAsteriskLabels(queryLabels: Set<String>, policyLabels: Set<String>): Boolean {
        return queryLabels.contains("*") || policyLabels.contains("*")
    }

    override fun matchLabelsExactly(
        queryLabels: Set<String>, policyLabels: Set<String>, matchEmptyQueryLabels: Boolean
    ): Boolean {
        return anyAsteriskLabels(
            queryLabels, policyLabels
        ) || queryLabels == policyLabels || matchEmptyQueryLabels && queryLabels.isEmpty()
    }

    override fun matchLabelsQueryContainsAllPolicy(
        queryLabels: Set<String>, policyLabels: Set<String>, matchEmptyQueryLabels: Boolean
    ): Boolean {
        return anyAsteriskLabels(
            queryLabels, policyLabels
        ) || queryLabels.containsAll(policyLabels) || matchEmptyQueryLabels && queryLabels.isEmpty()
    }

    override fun matchLabelsQueryContainsAnyPolicy(
        queryLabels: Set<String>, policyLabels: Set<String>, matchEmptyQueryLabels: Boolean
    ): Boolean {
        return anyAsteriskLabels(
            queryLabels, policyLabels
        ) || policyLabels.any { queryLabels.contains(it) } || matchEmptyQueryLabels && queryLabels.isEmpty()
    }
}
