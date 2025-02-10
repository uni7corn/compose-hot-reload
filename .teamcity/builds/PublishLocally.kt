/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package builds

import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

object PublishLocally : BuildType({
    name = "Publish: Locally"

    artifactRules = """
        build/repo/**/* => build/repo.zip
    """.trimIndent()

    steps {
        gradle {
            name = "Publish Locally"
            tasks = "publishLocally"
        }
    }
})
