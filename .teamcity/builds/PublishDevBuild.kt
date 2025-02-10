/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package builds

import builds.conventions.PublishDevPrivilege
import builds.conventions.PublishLocallyConvention
import builds.conventions.PushPrivilege
import builds.conventions.publishDevVersion
import builds.conventions.setupGit
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.ScheduleTrigger
import jetbrains.buildServer.configs.kotlin.triggers.schedule

object PublishDevBuild : BuildType({
    name = "Publish: Dev Build"
    description = "Bumps the 'dev' version and publishes to 'dev' repositories; Bumps the bootstrap version"

    triggers {
        schedule {
            weekly {
                dayOfWeek = ScheduleTrigger.DAY.Sunday
                dayOfWeek = ScheduleTrigger.DAY.Wednesday
                timezone = "Europe/Berlin"
                hour = 3
            }
        }
    }


    steps {
        setupGit()

        gradle {
            workingDir = "repository-tools"
            name = "Bump Dev Version"
            tasks = "bumpDevVersion"
        }

        publishDevVersion()

        gradle {
            workingDir = "repository-tools"
            name = "Bump Bootstrap Version"
            tasks = "bumpBootstrapVersion"
        }
    }
}), PushPrivilege, PublishDevPrivilege, PublishLocallyConvention
