package at.jku.faw.symspace.cypherrewriter.core.cypher.detector.matchutils

class PolicyContext(
    val labelMatchers: Map<String, (Set<String>, Set<String>) -> Boolean>
) : CommonContext()
