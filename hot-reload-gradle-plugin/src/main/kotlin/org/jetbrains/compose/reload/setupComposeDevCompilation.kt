package org.jetbrains.compose.reload

import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

internal fun Project.setupComposeDevCompilation() {
    kotlinMultiplatformOrNull?.targets?.withType<KotlinJvmTarget>()?.configureEach { target ->
        target.setupComposeDevCompilation()
    }

    kotlinJvmOrNull?.target?.setupComposeDevCompilation()
}

@OptIn(ExternalKotlinTargetApi::class)
private fun KotlinTarget.setupComposeDevCompilation() {
    val main = compilations.getByName("main")
    val dev = compilations.maybeCreate("dev")
    dev.associateWith(main)

    project.tasks.register("devRun", ComposeDevRun::class.java) { task ->
        val className = project.providers.systemProperty("className")
            .orElse(project.providers.gradleProperty("className"))

        val funName = project.providers.systemProperty("funName")
            .orElse(project.providers.gradleProperty("funName"))

        task.inputs.property("className", className)
        task.inputs.property("funName", funName)

        task.configureJavaExecTaskForHotReload(project.provider { dev })
        task.mainClass.set("org.jetbrains.compose.reload.jvm.DevApplication")

        task.doFirst {
            task.args("--className", className.get(), "--funName", funName.get())
        }
    }
}


internal open class ComposeDevRun : JavaExec()