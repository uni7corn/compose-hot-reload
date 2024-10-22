package org.jetbrains.compose.reload

import org.gradle.api.Project

internal fun Project.setupComposeHotReloadRuntimeDependency() {
    /*
    Multiplatform impl
     */
    kotlinMultiplatformOrNull?.apply {
        sourceSets.commonMain.dependencies {
            implementation("org.jetbrains.compose:hot-reload-runtime-api:$HOT_RELOAD_VERSION")
        }
    }

    /*
    Jvm impl
     */
    kotlinJvmOrNull?.apply {
        val compilation = target.compilations.getByName("main")
        compilation.defaultSourceSet.dependencies {
            implementation("org.jetbrains.compose:hot-reload-runtime:$HOT_RELOAD_VERSION")
        }
    }
}