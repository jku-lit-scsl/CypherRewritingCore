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
class ReturnTypeHelperImpl : ReturnTypeHelper {
    override fun mergeReturnTypes(existingReturnType: ReturnType, newReturnType: ReturnType): ReturnType {
        if (newReturnType == ReturnType.ANY) {
            throw IllegalArgumentException("$newReturnType must not be used here.")
        }

        return if (existingReturnType < newReturnType) {
            newReturnType
        } else {
            existingReturnType
        }
    }

    override fun returnTypesMatch(queryReturnType: ReturnType, policyReturnType: ReturnType): Boolean {
        return policyReturnType == ReturnType.ANY
                || queryReturnType == ReturnType.ANY
                || queryReturnType == policyReturnType
                || policyReturnType == ReturnType.ANY_RETURN && queryReturnType in ReturnTypeMetadata.anyReturnTypes
    }
}
