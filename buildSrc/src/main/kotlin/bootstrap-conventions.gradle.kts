/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


/*
   Required to enforce no accidental resolve to the -api-jvm, published by the runtime api project.
   Usually such substitutions are made automatically by Gradle, but there seems to be an issue
   with the sub-publications of the KMP project
   */
configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.jetbrains.compose.hot-reload:runtime-api"))
            .using(project(":hot-reload-runtime-api"))

        substitute(module("org.jetbrains.compose.hot-reload:runtime-api-jvm"))
            .using(project(":hot-reload-runtime-api"))
    }
}
