/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package builds

import builds.conventions.CommitStatusPublisher
import builds.conventions.HardwareCapacity
import builds.conventions.HostRequirement
import builds.conventions.PublishLocallyConvention
import builds.utils.Host
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildFeatures.buildCache
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

class Test(
    override val requiredHost: Host
) : BuildType({
    name = "Tests: $requiredHost"
    id("Tests_$requiredHost")

    artifactRules = """
        **/*-actual*
        **/build/reports/** => reports.zip
    """.trimIndent()

    features {
        buildCache {
            use = true
            publish = true
            name = "Functional Test Gradle Cache (modules-2)"
            rules = """
                tests/build/gradleHome/caches/modules-2/files-2.1
                tests/build/gradleHome/caches/modules-2/metadata-2.106
                tests/build/gradleHome/caches/modules-2/metadata-2.107
            """.trimIndent()
        }

        buildCache {
            use = true
            publish = true
            name = "Functional Test Gradle Cache (build-cache-2)"
            rules = """
                tests/build/gradleHome/caches/build-cache-1
            """.trimIndent()
        }


        buildCache {
            use = true
            publish = true
            name = "Functional Test Gradle Cache (wrapper)"
            rules = """
                tests/build/gradleHome/wrapper
            """.trimIndent()
        }
    }

    steps {
        gradle {
            name = "Test"
            tasks = "check -i --continue -x apiCheck -x publishLocally -Pchr.tests.parallelism=2"

            /* Any host other than linux is considered to only run 'host integration tests' */
            if (requiredHost != Host.Linux) {
                tasks += " -Phost-integration-tests=true"
            }
        }
    }
}), PublishLocallyConvention,
    CommitStatusPublisher,
    HostRequirement.Dynamic,
    HardwareCapacity.Large
