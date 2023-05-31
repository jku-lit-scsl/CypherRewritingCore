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

package at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils

data class Context(
    val policy: PolicyContext,
    val query: QueryContext = QueryContext(),
    val structuralMatches: MutableSet<StructuralMatch> = mutableSetOf(),
    val possibleVarMappings: MutableMap<Variable, MutableSet<Variable>> = mutableMapOf(),
    val validVarMappings: MutableSet<Mapping> = mutableSetOf(),
    val filteredVariables: MutableList<Variable> = mutableListOf(),
    val returnedVariables: MutableList<Variable> = mutableListOf(),
    var inReturnSubTree: Boolean = false,
)
