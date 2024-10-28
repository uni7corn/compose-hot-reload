package org.jetbrains.compose.reload

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.withType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.jetbrains.compose.reload.orchestration.OrchestrationClient
import org.jetbrains.compose.reload.orchestration.OrchestrationClientRole.Compiler
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ReloadClassesRequest
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ReloadClassesRequest.ChangeType
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import java.io.File


internal fun Project.setupComposeHotClasspathTasks() {
    kotlinMultiplatformOrNull?.targets?.all { target ->
        target.compilations.all { compilation -> setupComposeHotClasspathTask(compilation) }
    }

    kotlinJvmOrNull?.target?.compilations?.all { compilation -> setupComposeHotClasspathTask(compilation) }

    tasks.withType<ComposeHotClasspathTask>().configureEach { task ->
        task.outputs.upToDateWhen { true }
        task.group = "compose"
        task.agentPort.set(project.orchestrationPort)
    }
}

internal fun Project.setupComposeHotClasspathTask(compilation: KotlinCompilation<*>): TaskProvider<ComposeHotClasspathTask> {
    val name = composeHotClasspathTaskName(compilation)
    if (name in tasks.names) return tasks.named(name, ComposeHotClasspathTask::class.java)

    return tasks.register(name, ComposeHotClasspathTask::class.java) { task ->
        task.classpath.from(compilation.createComposeHotReloadRunClasspath())
    }
}

internal fun composeHotClasspathTaskName(compilation: KotlinCompilation<*>): String {
    return compilation.compileKotlinTaskName + "HotClasspath"
}

internal open class ComposeHotClasspathTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Incremental
    val classpath: ConfigurableFileCollection = project.objects.fileCollection()

    @Internal
    val agentPort = project.objects.property<Int>()


    @TaskAction
    fun execute(inputs: InputChanges) {
        val client = OrchestrationClient(Compiler) ?: error("Failed to create 'OrchestrationClient'!")

        if (!inputs.isIncremental) {
            logger.debug("Gradle Daemon is ready")
            client.use { client.sendMessage(OrchestrationMessage.GradleDaemonReady()) }
            return
        }

        logger.quiet("Incremental run")
        val changedClassFiles = mutableMapOf<File, ChangeType>()
        inputs.getFileChanges(classpath).forEach { change ->
            val changeType = when (change.changeType) {
                org.gradle.work.ChangeType.ADDED -> ChangeType.Added
                org.gradle.work.ChangeType.MODIFIED -> ChangeType.Modified
                org.gradle.work.ChangeType.REMOVED -> ChangeType.Removed
            }

            changedClassFiles[change.file.absoluteFile] = changeType
            logger.trace("[${change.changeType}] ${change.file}")
        }

        client.use {
            client.sendMessage(ReloadClassesRequest(changedClassFiles))
        }
    }
}
