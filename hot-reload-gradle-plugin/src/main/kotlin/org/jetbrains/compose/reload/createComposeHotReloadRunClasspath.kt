@file:Suppress("UnstableApiUsage")

package org.jetbrains.compose.reload

import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/**
 * We're trying to resolve the classpath to classes dirs:
 * Therefore, we're explicitly trying to resolve classes by looking out for a
 * [ComposeHotReloadMarker].
 *
 * Projects which also do have the hot-reload plugin applied will be able to provide
 * us with a better variant. Projects which do not have this plugin applied will provide
 * use with a regular variant.
 */
internal fun KotlinCompilation<*>.createComposeHotReloadRunClasspath(): FileCollection {
    val runtimeConfigurationName = runtimeDependencyConfigurationName ?: compileDependencyConfigurationName
    val runtimeConfiguration = project.configurations.getByName(runtimeConfigurationName)

    val dependencyProjects = runtimeConfiguration.incoming.artifactView { view ->
        view.componentFilter { id -> id.isCurrentBuild() }
        view.attributes.attribute(KotlinPlatformType.attribute, platformType)
        view.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(COMPOSE_HOT_RELOAD_USAGE))
        view.isLenient = false
    }.files

    val dependencyBinaries = runtimeConfiguration.incoming.artifactView { view ->
        view.componentFilter { id -> !id.isCurrentBuild() }
    }.files

    return project.files(
        output.allOutputs, dependencyProjects, dependencyBinaries
    )
}

private fun ComponentIdentifier.isCurrentBuild(): Boolean {
    @Suppress("DEPRECATION") // Copy approach from KGP?
    return this is ProjectComponentIdentifier &&
            build.isCurrentBuild
}