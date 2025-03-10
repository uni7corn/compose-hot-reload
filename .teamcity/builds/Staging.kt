/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package builds

import builds.conventions.HardwareCapacity
import builds.conventions.PushPrivilege
import builds.conventions.setupGit
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object StagingDeploy : BuildType({
    name = "Deploy Staging -> Master"
    type = Type.DEPLOYMENT


    vcs {
        branchFilter = """
            +:staging
            +:master
        """.trimIndent()
    }

    triggers {
        vcs {
            branchFilter = "+:staging"
        }
    }

    dependencies {
        snapshot(AllTests) {}
    }

    steps {
        setupGit()
        script {
            name = "Push"
            scriptContent = """
                git remote -v
                git log %build.vcs.number%
                git fetch --unshallow origin master
                git log -n 5 origin/master
                git push origin %build.vcs.number%:refs/heads/master -v
            """.trimIndent()
        }
    }
}), PushPrivilege, HardwareCapacity.Small
