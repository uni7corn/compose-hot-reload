/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package builds.conventions

import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildFeatures.buildCache
import jetbrains.buildServer.configs.kotlin.buildFeatures.gradleCache
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import vcs.Github

fun BuildType.configureConventions() {
    requireLinux()
    defaultVcs()
    defaultFeatures()
    defaultCaches()
    pushPrivilegeConventions()
    publishDevPrivilegeConventions()
    publishLocallyConventions()
}


private fun BuildType.defaultVcs() {
    vcs { root(Github) }
}

private fun BuildType.requireLinux() {
    requirements {
        matches("teamcity.agent.jvm.os.name", "Linux")
    }
}

private fun BuildType.defaultFeatures() {
    features {
        perfmon { }
        gradleCache { }
    }
}

private fun BuildType.defaultCaches() {
    features {
        buildCache {
            name = "buildSrc cache"
            rules = """
                buildSrc/build/**
                buildSrc/.gradle/**
                buildSrc/.kotlin/**
            """.trimIndent()
        }

        buildCache {
            name = "Repository Tools cache"
            rules = """
                repository-tools/build/**
                repository-tools/.gradle/**
                repository-tools/.kotlin/**
            """.trimIndent()
        }
    }
}
