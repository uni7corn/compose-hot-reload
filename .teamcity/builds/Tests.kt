/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package builds

import builds.conventions.CommitStatusPublisher
import builds.conventions.HardwareCapacity
import builds.conventions.HostRequirement
import builds.conventions.PublishLocallyConvention
import builds.utils.Host
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.buildCache
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.sequential
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import vcs.Github


object Tests : Project({
    name = "Tests"
    description = "Compose Hot Reload Tests"

    buildType(AllTests)

    val linuxTest = Test(Host.Linux)
    val windowsTest = Test(Host.Windows)

    buildType(linuxTest)
    buildType(windowsTest)
    buildType(ApiCheck)
    buildType(SamplesCheck)
    buildType(TestIntelliJPluginCheck)

    sequential {
        buildType(PublishLocally)
        parallel {
            buildType(windowsTest)
            buildType(linuxTest)
            buildType(ApiCheck)
            buildType(SamplesCheck)
            buildType(TestIntelliJPluginCheck)
        }
        buildType(AllTests)
    }
})

object AllTests : BuildType({
    name = "All Tests"
    description = "All tests"

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
            publish = true
            name = "Functional Test Gradle Cache"
            rules = """
                tests/build/gradleHome/
                tests/build/reloadFunctionalTestWarmup/
            """.trimIndent()
        }
    }

    steps {
        gradle {
            name = "Test"
            tasks = "check --continue -x apiCheck"

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
