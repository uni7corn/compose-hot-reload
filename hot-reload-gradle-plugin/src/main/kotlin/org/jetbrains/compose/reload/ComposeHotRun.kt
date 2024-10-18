package org.jetbrains.compose.reload

import org.gradle.api.Project
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

internal fun Project.setupComposeHotRunConventions() {
    tasks.withType<ComposeHotRun> {

        group = "run"
        description = "Compose Application Run (Hot Reload enabled) | -PmainClass=..."

        mainClass.convention(
            project.providers.gradleProperty("mainClass")
                .orElse(project.providers.systemProperty("mainClass"))
        )

        compilation.convention(provider {
            kotlinMultiplatformOrNull?.let { kotlin ->
                return@provider kotlin.targets.withType<KotlinJvmTarget>().first().compilations.getByName("main")
            }

            kotlinJvmOrNull?.let { kotlin ->
                return@provider kotlin.target.compilations.getByName("main")
            }
            null
        })

        configureJavaExecTaskForHotReload(compilation)
    }
}

/**
 * Registers a custom 'run' task
 * Example Usage
 *
 * ```
 * // build.gradle.kts
 *
 * tasks.register<ComposeHotRun>("runHot") {
 *     mainClass = "my.app.MainKt" // <- Optional: Can also be provided with -PmainClass
 * }
 *
 * ```
 */
open class ComposeHotRun : JavaExec() {
    @Transient
    @get:Internal
    val compilation = project.objects.property<KotlinCompilation<*>>()
}
