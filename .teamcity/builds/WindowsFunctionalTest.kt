/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package builds

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
            name = "(Windows) Android SDK"
            use = true
            publish = true
            rules = """
                .local/android-sdk
            """.trimIndent()
        }


        buildCache {
            name = "(Windows) .konan dependencies"
            use = true
            publish = true
            rules = """
               .local/konan/dependencies
           """.trimIndent()
        }

        buildCache {
            name = "(Windows) .konan prebuilt"
            use = true
            publish = true
            rules = """
                .local/konan/kotlin-native-prebuilt-windows-x86_64-2.1.20
            """.trimIndent()
        }

        buildCache {
            name = "(Windows) Gradle (Wrapper)"
            use = true
            publish = true
            rules = """
                .local/gradle/wrapper/
            """.trimIndent()
        }

        buildCache {
            use = true
            publish = true
            name = "(Windows) Gradle Cache (modules-2)"
            rules = """
                .local/gradle/caches/modules-2/files-2.1
                .local/gradle/caches/modules-2/metadata-2.106
                .local/gradle/caches/modules-2/metadata-2.107
            """.trimIndent()
        }

        buildCache {
            use = true
            publish = true
            name = "(Windows) Functional Test Gradle Cache (modules-2)"
            rules = """
                tests/build/gradleHome/caches/modules-2/files-2.1
                tests/build/gradleHome/caches/modules-2/metadata-2.106
                tests/build/gradleHome/caches/modules-2/metadata-2.107
            """.trimIndent()
        }
    }

    steps {
        gradle {
            name = "Install Android SDK"
            tasks = "installAndroidSdk"
            workingDir = "repository-tools"
        }

        gradle {
            name = "Bootstrap"
            tasks = "publishBootstrap -i"
            conditions {
                matches("bootstrap", "true")
            }
        }

        gradle {
            name = "Build"
            tasks = "publishLocally -i"
        }

        gradle {
            name = "Test"
            tasks = "reloadFunctionalTest -i --continue -x publishLocally " +
                "-Pchr.tests.sequential -Phost-integration-tests=true"
        }
    }
}), CommitStatusPublisher,
    HostRequirement.Windows,
    HardwareCapacity.Medium
