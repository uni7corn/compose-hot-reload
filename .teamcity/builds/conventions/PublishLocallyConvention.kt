/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package builds.conventions

import builds.utils.Host
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildFeatures.buildCache
import jetbrains.buildServer.configs.kotlin.buildSteps.GradleBuildStep

interface PublishLocallyConvention

fun BuildType.publishLocallyConventions() {
    if (this !is PublishLocallyConvention) return
    val thisBuildType = this

    artifactRules = """
        build/repo => repo.zip
    """.trimIndent()

    steps {
        items.add(0, GradleBuildStep {
            name = "Publish Locally"
            tasks = "publishLocally"
        })
    }

    features {
        if (thisBuildType.requiredHost == Host.Linux) {
            buildCache {
                use = true
                publish = true
                name = "Android SDK"
                rules = """
                    %android-sdk.location%/licenses
                    %android-sdk.location%/platforms
                    %android-sdk.location%/build-tools
                    """.trimIndent()
            }
        }

        buildCache {
            use = true
            publish = true
            name = "Gradle Build Cache"
            rules = """
                .local/build-cache
            """.trimIndent()
        }

        buildCache {
            use = true
            publish = true
            name = "${thisBuildType.name} Gradle Cache (caches)"
            rules = """
                .local/gradle/caches/modules-2/files-2.1
                .local/gradle/caches/modules-2/metadata-2.106
                .local/gradle/caches/modules-2/metadata-2.107
            """.trimIndent()
        }

        buildCache {
            use = true
            publish = true
            name = "Gradle Cache (wrapper)"
            rules = """
                .local/gradle/wrapper
            """.trimIndent()
        }
    }
}
