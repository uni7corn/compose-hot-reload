/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package builds.conventions

import builds.installAndroidSdkConvention
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon

fun BuildType.configureConventions() {
    vcsConventions()
    defaultFeatures()
    pushPrivilegeConventions()
    publishDevPrivilegeConventions()
    publishLocallyConventions()
    hostRequirementConventions()
    commitPublisherConventions()
    hardwareCapacity()
    installAndroidSdkConvention()
}


private fun BuildType.defaultFeatures() {
    features {
        perfmon { }
    }
}
