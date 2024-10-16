package org.jetbrains.compose.reload

import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

enum class ComposeUsage {
    Main, UI,
}

internal val COMPOSE_USAGE_ATTRIBUTE = Attribute.of(ComposeUsage::class.java)


internal fun KotlinCompilation<*>.createUIElementsConfigurations() {
    project.configurations.create("${target.name}${name.capitalized}ApiElements") {
        isCanBeConsumed = true
        isCanBeResolved = false

        extendsFrom(project.configurations.getByName(apiConfigurationName))

        attributes {
            attribute(KotlinPlatformType.attribute, target.platformType)
            attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_API))
            attribute(COMPOSE_USAGE_ATTRIBUTE, ComposeUsage.UI)
        }

        outgoing.artifact(project.provider { output.classesDirs.singleFile }) {
            builtBy(output.classesDirs)
        }
    }

    project.configurations.create("${target.name}${name.capitalized}RuntimeElements") {
        isCanBeConsumed = true
        isCanBeResolved = false

        val runtimeConfiguration = runtimeDependencyConfigurationName?.let { project.configurations.getByName(it) }
        if (runtimeConfiguration != null) extendsFrom(runtimeConfiguration)

        attributes {
            attribute(KotlinPlatformType.attribute, target.platformType)
            attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
            attribute(COMPOSE_USAGE_ATTRIBUTE, ComposeUsage.UI)
        }

        outgoing.artifact(project.provider { output.classesDirs.singleFile }) {
            builtBy(output.classesDirs)
        }

        outgoing.artifact(project.provider { output.resourcesDirProvider })
    }
}

