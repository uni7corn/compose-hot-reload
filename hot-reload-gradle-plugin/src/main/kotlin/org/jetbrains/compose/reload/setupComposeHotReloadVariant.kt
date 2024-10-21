package org.jetbrains.compose.reload

import org.gradle.api.Project
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails
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
    project.dependencies.attributesSchema.attribute(Usage.USAGE_ATTRIBUTE)
        .compatibilityRules.add(ComposeHotReloadCompatibility::class.java)

    kotlinJvmOrNull?.apply {
        target.createComposeHotReloadVariants()
    }

    kotlinMultiplatformOrNull?.apply {
        targets.withType<KotlinJvmTarget>().all { target ->
            target.createComposeHotReloadVariants()
        }
    }
}

internal const val COMPOSE_HOT_RELOAD_USAGE = "compose-hot-reload-runtime"

internal class ComposeHotReloadCompatibility : AttributeCompatibilityRule<Usage> {
    override fun execute(details: CompatibilityCheckDetails<Usage>) {
        if (details.consumerValue?.name == COMPOSE_HOT_RELOAD_USAGE &&
            details.producerValue?.name == Usage.JAVA_RUNTIME
        ) {
            details.compatible()
        }
    }
}

private fun KotlinTarget.createComposeHotReloadVariants() {
    val main = compilations.getByName("main")
    val runtimeElements = project.configurations.getByName(runtimeElementsConfigurationName)

    runtimeElements.outgoing.also outgoing@{ outgoing ->
        project.logger.debug("Creating 'composeHot' variant")

        if (outgoing.variants.findByName("composeHot") != null) {
            project.logger.error("Could not create 'composeHot' variant: Variant already exists!", Throwable())
            return@outgoing
        }

        outgoing.variants.create("composeHot") { variant ->
            variant.attributes.attribute(KotlinPlatformType.attribute, platformType)
            variant.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(COMPOSE_HOT_RELOAD_USAGE))

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
