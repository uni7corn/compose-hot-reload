/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.compose.reload.tests.gradle

import kotlinx.coroutines.flow.toList
import org.jetbrains.compose.reload.test.gradle.AndroidHotReloadTest
import org.jetbrains.compose.reload.test.gradle.ExtendGradleProperties
import org.jetbrains.compose.reload.test.gradle.GradlePropertiesExtension
import org.jetbrains.compose.reload.test.gradle.HotReloadTest
import org.jetbrains.compose.reload.test.gradle.HotReloadTestFixture
import org.jetbrains.compose.reload.test.gradle.TestedGradleVersion
import org.jetbrains.compose.reload.test.gradle.assertSuccessful
import org.jetbrains.compose.reload.test.gradle.buildFlow
import org.jetbrains.compose.reload.test.gradle.testedGradleVersion
import org.jetbrains.compose.reload.utils.GradleIntegrationTest
import org.jetbrains.compose.reload.utils.QuickTest
import org.junit.jupiter.api.extension.ExtensionContext

@AndroidHotReloadTest
@GradleIntegrationTest
@QuickTest
@ExtendGradleProperties(IsolatedProjectsEnabledSmokeTest.Extension::class)
class IsolatedProjectsEnabledSmokeTest {

    @HotReloadTest
    fun `test - tasks - isolated projects enabled`(fixture: HotReloadTestFixture) = fixture.runTest {
        gradleRunner.buildFlow("tasks").toList().assertSuccessful()
    }

    class Extension : GradlePropertiesExtension {
        val minGradleVersion = TestedGradleVersion("8.7")

        override fun properties(context: ExtensionContext): List<String> = when {
            context.testedGradleVersion <= minGradleVersion -> emptyList()
            else -> listOf("compose.reload.isolatedProjectsEnabled=true")
        }

        private operator fun TestedGradleVersion.compareTo(other: TestedGradleVersion): Int {
            val thisVersions = version.split(".").map {
                it.toIntOrNull() ?: error("Unable to parse Gradle version component: $it")
            }
            val otherVersions = other.version.split(".").map {
                it.toIntOrNull() ?: error("Unable to parse Gradle version component: $it")
            }
            val maxIndex = maxOf(thisVersions.size, otherVersions.size)
            for (index in 0..<maxIndex) {
                val thisValue = thisVersions.getOrElse(index) { 0 }
                val otherValue = otherVersions.getOrElse(index) { 0 }
                if (thisValue != otherValue) return thisValue.compareTo(otherValue)
            }
            return 0
        }
    }
}
