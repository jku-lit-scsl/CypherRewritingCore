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

