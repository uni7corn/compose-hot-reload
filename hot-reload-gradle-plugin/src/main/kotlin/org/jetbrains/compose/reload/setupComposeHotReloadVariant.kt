package org.jetbrains.compose.reload

import org.gradle.api.Project
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.CompatibilityCheckDetails
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
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
        target.createComposeHotReloadRuntimeElements()
    }

    kotlinMultiplatformOrNull?.apply {
        targets.withType<KotlinJvmTarget>().all { target ->
            target.createComposeHotReloadRuntimeElements()
        }
    }
}

internal const val COMPOSE_DEV_RUNTIME_USAGE = "compose-dev-java-runtime"

internal class ComposeHotReloadCompatibility : AttributeCompatibilityRule<Usage> {
    override fun execute(details: CompatibilityCheckDetails<Usage>) {
        if (details.consumerValue?.name == COMPOSE_DEV_RUNTIME_USAGE &&
            details.producerValue?.name == Usage.JAVA_RUNTIME
        ) {
            details.compatible()
        }
    }
}

private fun KotlinTarget.createComposeHotReloadRuntimeElements() {
    val main = compilations.getByName("main")
    val runtimeConfigurationName = main.runtimeDependencyConfigurationName + "ComposeHot"
    project.configurations.findByName(runtimeConfigurationName)?.let { return }
        ?: project.configurations.create(runtimeConfigurationName) { configuration ->

            configuration.attributes.attribute(KotlinPlatformType.attribute, platformType)
            configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(COMPOSE_DEV_RUNTIME_USAGE))
            configuration.attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
            configuration.attributes.attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.objects.named(STANDARD_JVM))

            configuration.isCanBeResolved = false
            configuration.isCanBeConsumed = true

            project.afterEvaluate {
                main.output.classesDirs.forEach { classesDir ->
                    configuration.outgoing.artifact(classesDir) { artifact ->
                        artifact.builtBy(main.output.allOutputs)
                        artifact.builtBy(main.compileTaskProvider)
                    }
                }
            }

            configuration.outgoing.artifact(project.provider { main.output.resourcesDirProvider }) { artifact ->
                artifact.builtBy(main.output.allOutputs)
                artifact.builtBy(main.compileTaskProvider)
            }
        }
}
