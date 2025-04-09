/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.compose.reload

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Sync
import org.gradle.internal.component.local.model.OpaqueComponentArtifactIdentifier
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.compose.reload.core.HOT_RELOAD_VERSION
import org.jetbrains.compose.reload.gradle.capitalized
import org.jetbrains.compose.reload.gradle.composeHotReloadAgentRuntimeClasspath
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

private const val hotReloadRuntimeConfigurationName = "hotReloadRuntime"

/**
 * This configuration only contains the 'dev' variant of the 'hot-reload:runtime-*' artifacts.
 */
internal val Project.hotReloadRuntimeConfiguration: Configuration
    get() = project.configurations.findByName(hotReloadRuntimeConfigurationName)
        ?: project.configurations.create(hotReloadRuntimeConfigurationName) { configuration ->
            configuration.isCanBeResolved = true
            configuration.isCanBeConsumed = false
            configuration.isVisible = false

            configuration.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
            configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(COMPOSE_DEV_RUNTIME_USAGE))
            configuration.attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
            configuration.attributes.attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.objects.named(STANDARD_JVM))

            /**
             * The dev runtime should also include the 'runtime-api,' which will resolve to the dev variant,
             * practically engaging the 'DevelopmentEntryPoint {}' transformations
             */
            project.dependencies.add(
                configuration.name,
                "org.jetbrains.compose.hot-reload:runtime-api:$HOT_RELOAD_VERSION"
            )
        }

/**
 * Contains the 'dev' variant of the 'hot reload runtime':
 * Aka. the version of the runtime which is required for running in hot reload mode.
 */
val Project.hotReloadRuntimeClasspath: FileCollection get() = hotReloadRuntimeConfiguration

/**
 * The raw runtime classpath: This will be the direct output of the
 * compilations inside this project. This is the classpath, which will
 * change and can be tracked.
 *
 * Note: The [hotRuntimeClasspath] is used to later construct the
 * [hotApplicationClasspath]. The [hotApplicationClasspath] will then be
 * used to actually run the application against.
 */
internal val KotlinCompilation<*>.hotRuntimeClasspath: FileCollection by lazyProperty {
    val projectDependencies = composeDevRuntimeDependencies.incoming.artifactView { view ->
        view.componentFilter { id -> id.isCurrentBuild() }
        view.attributes { attributes ->
            attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
        }
    }.files

    /**
     * Associated compilations can add opaque dependencies, which we will still
     * consider to be hot!
     */
    val opaqueDirectoryDependencies = composeDevRuntimeDependencies.incoming.artifactView { view ->
        view.componentFilter { id -> id is OpaqueComponentArtifactIdentifier && id.file.isDirectory }
    }.files

    project.files(output.allOutputs, projectDependencies, opaqueDirectoryDependencies)
}

/**
 * The part of the classpath which is known to never change (e.g. binary
 * dependencies downloaded from the network)
 */
internal val KotlinCompilation<*>.coldRuntimeClasspath: FileCollection by lazyProperty {
    composeDevRuntimeDependencies.incoming.artifactView { view ->
        view.componentFilter { id ->
            if (id.isCurrentBuild()) return@componentFilter false
            if (id is OpaqueComponentArtifactIdentifier && id.file.isDirectory) return@componentFilter false
            true
        }
    }.files
}

/**
 * The classpath used to start the application with. This will be
 * constructed from several parts:
 * - compose reload agent classpath
 * - [hotApplicationClasspath]
 * - [coldRuntimeClasspath]
 */
internal val KotlinCompilation<*>.applicationClasspath: FileCollection by lazyProperty {
    project.files(
        project.composeHotReloadAgentRuntimeClasspath(),
        hotApplicationClasspath,
        coldRuntimeClasspath
    )
}

/**
 * Should contain the same content as the [hotRuntimeClasspath]. These
 * files are used to actually run the application with. The files will be
 * copied to a given directory before actually starting the application.
 * This protects the classes, which are used by the application, from
 * being affected by re-compiling (which might delete said classes).
 */
internal val KotlinCompilation<*>.hotApplicationClasspath: FileCollection by lazyProperty {
    val targetDirectory = runBuildDirectory("classes")

    val syncTask = project.tasks.register(
        "sync${target.name}${name.capitalized}ApplicationClasses", Sync::class.java
    ) { task ->
        val hotRuntimeClasspath = hotRuntimeClasspath
        task.from(hotRuntimeClasspath) { it.duplicatesStrategy = DuplicatesStrategy.WARN }
        task.into(targetDirectory)
    }

    project.files(targetDirectory).builtBy(syncTask)
}


private fun ComponentIdentifier.isCurrentBuild(): Boolean {
    @Suppress("DEPRECATION") // Copy approach from KGP?
    return this is ProjectComponentIdentifier && build.isCurrentBuild
}
