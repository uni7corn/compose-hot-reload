package org.jetbrains.compose.reload

import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

/**
 * Hot Reloading works significantly better if only class directories are used.
 * Therefore, this method will create additional variants which will provide the classes dirs directly
 * as outgoing runtime elements.
 */
internal fun Project.setupComposeHotReloadVariant() {
    kotlinJvmOrNull?.apply {
        target.createComposeHotReloadVariants()
    }

    kotlinMultiplatformOrNull?.apply {
        targets.withType<KotlinJvmTarget>().all { target ->
            target.createComposeHotReloadVariants()
        }
    }
}

private fun KotlinTarget.createComposeHotReloadVariants() {
    val main = compilations.getByName("main")
    val runtimeElements = project.configurations.getByName(runtimeElementsConfigurationName)

    runtimeElements.outgoing { outgoing ->
        outgoing.attributes.attribute(ComposeHotReloadMarker.attribute, ComposeHotReloadMarker.Cold)

        project.logger.debug("Creating 'composeHot' variant")

        if (outgoing.variants.findByName("composeHot") != null) {
            project.logger.error("Could not create 'composeHot' variant: Variant already exists!", Throwable())
            return@outgoing
        }

        outgoing.variants.create("composeHot") { variant ->
            variant.attributes.attribute(KotlinPlatformType.attribute, platformType)
            variant.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
            variant.attributes.attribute(ComposeHotReloadMarker.attribute, ComposeHotReloadMarker.Hot)

            variant.artifact(project.provider { main.output.classesDirs.singleFile }) { artifact ->
                artifact.builtBy(main.output.allOutputs)
                artifact.builtBy(main.compileTaskProvider)
            }

            variant.artifact(project.provider { main.output.resourcesDirProvider }) { artifact ->
                artifact.builtBy(main.output.allOutputs)
                artifact.builtBy(main.compileTaskProvider)
            }
        }
    }
}
