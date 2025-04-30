/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package builds

import builds.conventions.BuildCacheConvention
import builds.conventions.CommitStatusPublisher
import builds.conventions.HardwareCapacity
import builds.conventions.HostRequirement
import builds.conventions.requiredHost
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildFeatures.buildCache
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

class WindowsFunctionalTest() : BuildType({
    name = "Functional Test: $requiredHost"
    id("FunctionalTest_$requiredHost")

    params {
        param("bootstrap", "false")
        param("env.ANDROID_HOME", "%system.teamcity.build.workingDir%/.local/android-sdk")
    }

    artifactRules = """
        **/*-actual*
        **/build/reports/** => reports.zip
    """.trimIndent()

    features {
        buildCache {
            use = true
            publish = true
            name = "(Windows) Functional Test Gradle (build-cache)"
            rules = """
                tests/build/gradleHome/caches/build-cache-1
            """.trimIndent()
        }
    }

    steps {
        gradle {
            name = "Bootstrap"
            tasks = "publishBootstrap -i"
            conditions {
                matches("bootstrap", "true")
            }
        }

        gradle {
            name = "Test"
            tasks = "reloadFunctionalTest -i --continue " +
                "-Pchr.tests.sequential -Phost-integration-tests=true"
        }
    }
}), CommitStatusPublisher,
    HostRequirement.Windows,
    HardwareCapacity.Large,
    BuildCacheConvention.Consumer
