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
import jetbrains.buildServer.configs.kotlin.BuildTypeSettings
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.buildCache
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.sequential
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import vcs.Github
import kotlin.time.Duration.Companion.minutes

object AllTests : BuildType({
    name = "All Tests"
    description = "All tests"
    type = Type.COMPOSITE

    features {
        pullRequests {
            vcsRootExtId = Github.id.toString()
            provider = github {
                authType = token {
                    token = "credentialsJSON:63ad183a-fe82-4c2f-b80d-f92b2d7b69ec"
                }
                filterAuthorRole = PullRequests.GitHubRoleFilter.MEMBER
            }
        }
    }

    triggers {
        vcs {
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_CUSTOM
            quietPeriod = 15.minutes.inWholeSeconds.toInt()
            branchFilter = """
                +:<default>
                +:master
                +:rr/*
                +pr: target=master sourceRepo=same github_role=member draft=false
            """.trimIndent()
        }
    }
})

class Test(
    override val requiredHost: Host
) : BuildType({
    name = "Tests: $requiredHost"
    id("Tests_$requiredHost")

    artifactRules = """
        **/*-actual*
        **/build/reports/**
    """.trimIndent()


    features {
        buildCache {
            use = true
            publish = true
            name = "Functional Test Gradle Cache (caches)"
            rules = """
                tests/build/gradleHome/caches
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
            tasks = "check -i --continue -x apiCheck -x publishLocally -Pchr.tests.parallelism=4"

            /* Any host other than linux is considered to only run 'host integration tests' */
            if (requiredHost != Host.Linux) {
                tasks += " -Phost-integration-tests=true"
            }
        }
    }
}), PublishLocallyConvention,
    CommitStatusPublisher,
    HostRequirement.Dynamic,
    HardwareCapacity.XLarge

object ApiCheck : BuildType({
    name = "Check: api"
    description = "Checks API compatibility"

    steps {
        gradle {
            name = "Check API"
            tasks = "apiCheck"
        }
    }

}), CommitStatusPublisher, PublishLocallyConvention, HardwareCapacity.Small

object SamplesCheck : BuildType({
    name = "Check: sample projects"
    description = "Checks samples"

    steps {
        gradle {
            name = "Check: counter"
            tasks = "check"
            workingDir = "samples/counter"
        }

        gradle {
            name = "Check: bytecode-analyzer"
            tasks = "check"
            workingDir = "samples/bytecode-analyzer"
        }
    }
}), CommitStatusPublisher, PublishLocallyConvention, HardwareCapacity.Small


object TestIntelliJPluginCheck : BuildType({
    name = "Check: hot-reload-test/idea-plugin"

    steps {
        gradle {
            name = "Check: hot-reload-test: IntelliJ plugin"
            tasks = "check"
            workingDir = "hot-reload-test/idea-plugin"
        }
    }
}), CommitStatusPublisher, PublishLocallyConvention, HardwareCapacity.Small
