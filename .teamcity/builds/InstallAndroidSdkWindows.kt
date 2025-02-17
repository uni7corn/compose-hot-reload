/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package builds

import builds.conventions.HostRequirement
import builds.conventions.requiredHost
import builds.utils.Host
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object InstallAndroidSdkWindows : BuildType({
    name = "Util: Install Android SDK (Windows)"

    artifactRules = """
        .local/android-sdk => android-sdk.zip
    """.trimIndent()

    steps {
        gradle {
            name = "Install Android SDK"
            tasks = "installAndroidSdk"
            workingDir = "repository-tools"
        }
    }
}), HostRequirement.Windows


fun BuildType.installAndroidSdkConvention() {
    if (requiredHost != Host.Windows) return
    if (this is InstallAndroidSdkWindows) return

    params {
        param("env.ANDROID_HOME", "%system.teamcity.build.workingDir%\\.local\\android-sdk")
    }

    dependencies {
        dependency(InstallAndroidSdkWindows) {
            snapshot {
            }

            artifacts {
                artifactRules = """
                    android-sdk.zip!** => .local/android-sdk
                """.trimIndent()
            }
        }
    }
}
