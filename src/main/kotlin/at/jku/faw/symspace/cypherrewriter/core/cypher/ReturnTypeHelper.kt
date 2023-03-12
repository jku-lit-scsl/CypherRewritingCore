package at.jku.faw.symspace.cypherrewriter.core.cypher

interface ReturnTypeHelper {
    fun mergeReturnTypes(existingReturnType: ReturnType, newReturnType: ReturnType): ReturnType
    fun returnTypesMatch(queryReturnType: ReturnType, policyReturnType: ReturnType): Boolean
}
