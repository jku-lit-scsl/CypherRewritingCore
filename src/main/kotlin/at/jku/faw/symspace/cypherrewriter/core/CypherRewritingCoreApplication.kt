package at.jku.faw.symspace.cypherrewriter.core

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["at.jku.faw.symspace.cypherrewriter"])
class CypherRewritingCoreApplication

fun main(args: Array<String>) {
    runApplication<CypherRewritingCoreApplication>(*args)
}
