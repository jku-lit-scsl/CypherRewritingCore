package at.jku.faw.symspace.cypherrewriter.core.cypher.enforcer

import at.jku.faw.symspace.cypherrewriter.core.cypher.detector.Detection

interface CypherEnforcer {
    fun enforce(detections: Collection<Detection>)
}
