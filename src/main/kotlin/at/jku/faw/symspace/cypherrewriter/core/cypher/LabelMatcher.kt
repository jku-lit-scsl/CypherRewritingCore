package at.jku.faw.symspace.cypherrewriter.core.cypher

interface LabelMatcher {
    fun buildLabelMatchMap(patternMatchStrategy: Map<String, PatternMatchStrategy>): MutableMap<String, (Set<String>, Set<String>) -> Boolean>

    fun anyAsteriskLabels(queryLabels: Set<String>, policyLabels: Set<String>): Boolean
    fun matchLabelsExactly(queryLabels: Set<String>, policyLabels: Set<String>, matchEmptyQueryLabels: Boolean = false): Boolean
    fun matchLabelsQueryContainsAllPolicy(queryLabels: Set<String>, policyLabels: Set<String>, matchEmptyQueryLabels: Boolean = false): Boolean
    fun matchLabelsQueryContainsAnyPolicy(queryLabels: Set<String>, policyLabels: Set<String>, matchEmptyQueryLabels: Boolean = false): Boolean
}
