/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package builds

import builds.conventions.PushPrivilege
import builds.conventions.setupGit
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object PublishToMavenCentralProject : Project({
    name = "Publish: Maven Central"
    description = "Build configuration for publishing to Maven Central"

    buildType(DeployToMavenCentral)
})


object DeployToMavenCentral : BuildType({
    name = "Deploy: Deploy Bundle"

    vcs {
        cleanCheckout = true
    }

    artifactRules = """
            build/mavenCentral.deploy-*.zip => /
            build/mavenCentral.deploy.id.txt => /
        """.trimIndent()

    steps {
        setupGit()

        gradle {
            name = "Check API"
            tasks = "apiCheck"
        }

        gradle {
            name = "Deploy"
            tasks = "deployMavenCentralDeployBundle"
        }
    }
}), PushPrivilege
