package org.jetbrains.compose.reload.core.testFixtures

import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled
import org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.util.AnnotationUtils.findAnnotation
import kotlin.jvm.optionals.getOrNull


@ExtendWith(MinFireworkVersionCondition::class)
annotation class MinFireworkVersion(val version: String)

internal class MinFireworkVersionCondition : ExecutionCondition {
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val minFireworkVersionString = findAnnotation(context.testMethod, MinFireworkVersion::class.java)
            ?.getOrNull()?.version
            ?: return enabled("No @MinFireworkVersion annotation")

        val minFireworkVersion = KotlinToolingVersion(minFireworkVersionString)
        val fireworkVersion = KotlinToolingVersion(TestEnvironment.fireworkVersion ?: error("Missing firework.version"))

        return if (fireworkVersion >= minFireworkVersion) {
            enabled("Firework version $minFireworkVersion is greater or equal to $fireworkVersion")
        } else {
            disabled("Firework version $minFireworkVersion is less than $fireworkVersion")
        }
    }
}