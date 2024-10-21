package org.jetbrains.compose.reload

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.withType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import java.net.InetAddress
import java.net.Socket


internal fun Project.setupComposeHotClasspathTasks() {
    kotlinMultiplatformOrNull?.targets?.all { target ->
        target.compilations.all { compilation -> setupComposeHotClasspathTask(compilation) }
    }

    kotlinJvmOrNull?.target?.compilations?.all { compilation -> setupComposeHotClasspathTask(compilation) }

    tasks.withType<ComposeHotClasspathTask>().configureEach { task ->
        task.outputs.upToDateWhen { true }
        task.group = "compose"
        task.agentPort.set(project.providers.systemProperty("compose.hot.reload.agent.port").map { it.toInt() })
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
        if (inputs.isIncremental) {
            logger.quiet("Incremental run")
        }

        val output = StringBuilder()
        inputs.getFileChanges(classpath).forEach { change ->
            val formattedChange = "[${change.changeType}] ${change.file}"
            output.appendLine(formattedChange)
            logger.quiet(formattedChange)
        }

        // Sending instructions to agent!
        Socket(InetAddress.getLocalHost(), agentPort.get()).getOutputStream().use { out ->
            out.bufferedWriter().use { writer ->
                writer.write(output.toString())
                writer.flush()
            }
        }
    }
}
