@file:Suppress("UnstableApiUsage")

package org.jetbrains.compose.reload

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.gradle.api.file.FileCollection
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.tooling.core.extrasLazyProperty

internal val KotlinCompilation<*>.composeDevRuntimeDependencies: Configuration by extrasLazyProperty("composeDevRuntimeDependencies") {
    val runtimeConfigurationName = runtimeDependencyConfigurationName ?: compileDependencyConfigurationName
    val runtimeConfiguration = project.configurations.getByName(runtimeConfigurationName)

    project.configurations.create(runtimeConfigurationName + "ComposeDev").apply {
        extendsFrom(runtimeConfiguration)
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(COMPOSE_DEV_RUNTIME_USAGE))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
        attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
        attributes.attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.objects.named(TargetJvmEnvironment.STANDARD_JVM))
    }
}

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
    val dependencyProjects = composeDevRuntimeDependencies.incoming.artifactView { view ->
        view.componentFilter { id -> id.isCurrentBuild() }
        view.isLenient = false
    }.files

    val dependencyBinaries = composeDevRuntimeDependencies.incoming.artifactView { view ->
        view.componentFilter { id -> !id.isCurrentBuild() }
    }.files

    return project.files(
        output.allOutputs, dependencyProjects, dependencyBinaries
    )
}

private fun ComponentIdentifier.isCurrentBuild(): Boolean {
    @Suppress("DEPRECATION") // Copy approach from KGP?
    return this is ProjectComponentIdentifier && build.isCurrentBuild
}