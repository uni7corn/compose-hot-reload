/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

@file:Suppress("NullableBooleanElvis")

package org.jetbrains.compose.reload

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.reload.core.HOT_RELOAD_VERSION
import org.jetbrains.compose.reload.core.HotReloadProperty
import org.jetbrains.compose.reload.core.LaunchMode
import org.jetbrains.compose.reload.core.issueNewDebugSessionJvmArguments
import org.jetbrains.compose.reload.gradle.Future
import org.jetbrains.compose.reload.gradle.PluginStage
import org.jetbrains.compose.reload.gradle.await
import org.jetbrains.compose.reload.gradle.forAllJvmTargets
import org.jetbrains.compose.reload.gradle.future
import org.jetbrains.compose.reload.gradle.futureProvider
import org.jetbrains.compose.reload.gradle.kotlinJvmOrNull
import org.jetbrains.compose.reload.gradle.kotlinMultiplatformOrNull
import org.jetbrains.compose.reload.gradle.launch
import org.jetbrains.compose.reload.gradle.projectFuture
import org.jetbrains.compose.reload.gradle.string
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

@OptIn(InternalKotlinGradlePluginApi::class)
internal val Project.hotRunTasks: Future<Collection<TaskProvider<out AbstractComposeHotRun>>> by projectFuture {
    PluginStage.EagerConfiguration.await()

    /* Configure task conventions */
    tasks.withType<AbstractComposeHotRun>().configureEach { task ->
        task.group = "run"
        task.configureJavaExecTaskForHotReload(task.compilation)


        /* Wire up the dependency to the 'snapshot' task */
        task.dependsOn(task.snapshotTaskName)
        task.snapshotTaskName.set(project.provider {
            val compilation = task.compilation.orNull ?: return@provider null
            compilation.hotSnapshotTaskName
        })
    }

    tasks.withType<ComposeHotRun>().configureEach { task ->
        task.description = "Compose Application Run (Hot Reload enabled) | -PmainClass=..."

        task.mainClass.convention(mainClassConvention)

        task.compilation.convention(provider {
            kotlinMultiplatformOrNull?.let { kotlin ->
                return@provider kotlin.targets.withType<KotlinJvmTarget>()
                    .firstOrNull()?.compilations?.getByName("main")
            }

            kotlinJvmOrNull?.let { kotlin ->
                return@provider kotlin.target.compilations.getByName("main")
            }
            null
        })
    }

    /* Configure the dev run: Expect -DclassName and -DfunName */
    tasks.withType<ComposeDevRun>().configureEach { task ->
        task.description = "Compose Application Dev Run (Hot Reload enabled) | -PclassName=... -PfunName=..."
        task.mainClass.set("org.jetbrains.compose.reload.jvm.DevApplication")
        task.args("--className", task.className.string(), "--funName", task.funName.string())
    }

    val hotRunTasks = forAllJvmTargets { target -> target.hotRunTask }
    val devRunTasks = hotDevCompilations.await().map { compilation -> compilation.hotDevTask }

    (hotRunTasks + devRunTasks).mapNotNull { it.await() }
}

internal val KotlinTarget.hotRunTask: Future<TaskProvider<ComposeHotRun>?> by future {
    val mainCompilation = compilations.findByName("main") ?: return@future null
    project.tasks.register(mainCompilation.hotRunTaskName, ComposeHotRun::class.java) { task ->
        task.compilation.set(mainCompilation)
    }
}

internal val KotlinCompilation<*>.hotDevTask: Future<TaskProvider<ComposeDevRun>> by future {
    project.tasks.register(hotDevTaskName, ComposeDevRun::class.java) { task ->
        task.compilation.set(this)
    }
}

internal fun JavaExec.configureJavaExecTaskForHotReload(compilation: Provider<KotlinCompilation<*>>) {
    classpath = project.files(compilation.map { it.composeHotReloadRuntimeClasspath })

    val argfile = if (this is AbstractComposeHotRun) this.argFile
    else compilation.flatMap { compilation -> compilation.argFile }

    val pidfile = if (this is AbstractComposeHotRun) this.pidFile
    else compilation.flatMap { compilation -> compilation.pidFile }

    val isRecompileContinuous = if (this is AbstractComposeHotRun) this.isRecompileContinuous
    else project.provider { true }

    val hotReloadTaskName = compilation.map { compilation -> compilation.hotReloadTaskName }
    val hotReloadLifecycleTaskName = project.futureProvider { project.hotReloadLifecycleTask.await()?.name }

    withComposeHotReloadArguments {
        setMainClass(mainClass)
        setPidFile(pidfile.map { it.asFile })
        setArgFile(argfile.map { it.asFile })
        isRecompileContinuous(isRecompileContinuous)
        setReloadTaskName(compilation.map { compilation -> compilation.hotReloadTaskName })
    }

    project.launch {
        val processManager = project.hotProcessManagerTask.await().get()
        dependsOn(processManager)
        processManager.pidFiles.from(pidfile)
    }

    val intellijDebuggerDispatchPort = project.providers
        .environmentVariable(HotReloadProperty.IntelliJDebuggerDispatchPort.key)
        .orNull?.toIntOrNull()

    val projectPath = project.path

    doFirst {
        if (!mainClass.isPresent) {
            throw IllegalArgumentException(ErrorMessages.missingMainClassProperty(name))
        }


        if (intellijDebuggerDispatchPort != null) {
            /*
            Provisioning a new debug session. This will return jvm args for the debug agent.
            Since we would like to debug our hot reload agent, we ensure that the debug agent is listed first.
             */
            jvmArgs = issueNewDebugSessionJvmArguments(name, intellijDebuggerDispatchPort).toList() + jvmArgs.orEmpty()
        }

        /*
        Create and write the 'argfile in case this is not a hot reload run task;
        ComposeHotRun tasks will have a dedicated task to create this argfile
        */
        if (this !is AbstractComposeHotRun) {
            argfile.orNull?.asFile?.toPath()?.createArgfile(allJvmArgs, classpath.files)
        }

        jvmArgs = jvmArgs.orEmpty() + "-D${HotReloadProperty.LaunchMode.key}=${LaunchMode.GradleBlocking.name}"


        logger.quiet(buildString {
            append(
                """
              ________________________________________________________________________________________________
            | 
            | Compose Hot Reload ($HOT_RELOAD_VERSION)
            | Running '${mainClass.get()}'
            | JetBrains Runtime: ${javaLauncher.orNull?.executablePath?.asFile?.path}
            | ________________________________________________________________________________________________
            """.trimIndent()
            )

            if (!isRecompileContinuous.get()) {
                val hotReloadTaskPath = projectPath.takeIf { it != ":" }.orEmpty() + ":${hotReloadTaskName.get()}"
                val thisTaskPath = projectPath.takeIf { it != ":" }.orEmpty() + ":${name}"

                appendLine()
                append(
                    """
                     | Explicit Reload Mode:
                     | Use `./gradlew ${hotReloadLifecycleTaskName.get()}` to hot reload the application
                     | Use `./gradlew $hotReloadTaskPath` to hot reload the application (alternative)
                     | Use `./gradlew $thisTaskPath --auto` to automatically reload the application on source changes
                     | ________________________________________________________________________________________________
                """.trimIndent()
                )
            } else {
                appendLine()
                append(
                    """
                     | Auto Reload Mode:
                     | Compose Hot Reload will launch a separate Gradle Daemon 
                     | to continuously recompile and reload the application when source code is changed.
                     | ________________________________________________________________________________________________
                """.trimIndent()
                )
            }
        })
    }
}
