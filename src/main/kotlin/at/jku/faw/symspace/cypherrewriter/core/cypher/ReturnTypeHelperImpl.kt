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
