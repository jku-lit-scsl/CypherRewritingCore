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
