/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.compose.reload.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.initialization.BuildCancellationToken
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.withType
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.compose.reload.InternalHotReloadApi
import org.jetbrains.compose.reload.core.asTemplateOrThrow
import org.jetbrains.compose.reload.core.leftOr
import org.jetbrains.compose.reload.core.renderOrThrow
import org.jetbrains.compose.reload.orchestration.OrchestrationClient
import org.jetbrains.compose.reload.orchestration.OrchestrationClientRole.Compiler
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ReloadClassesRequest
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ReloadClassesRequest.ChangeType.Added
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ReloadClassesRequest.ChangeType.Modified
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ReloadClassesRequest.ChangeType.Removed
import org.jetbrains.compose.reload.orchestration.connectBlocking
import org.jetbrains.compose.reload.orchestration.sendBlocking
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import javax.inject.Inject
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

internal val Project.hotReloadLifecycleTask: Future<TaskProvider<ComposeHotReloadLifecycleTask>?> by projectFuture {
    PluginStage.EagerConfiguration.await()
    val name = "reload"
    if (name in tasks.names) {
        logger.error("Conflicting '$name' task detected")
        return@projectFuture null
    }

    tasks.register(name, ComposeHotReloadLifecycleTask::class.java) { task ->
        task.description = "Hot Reloads code for all running applications"
        task.dependsOn(tasks.withType<ComposeHotReloadTask>().matching { it.agentPort.isPresent })
    }
}

internal val Project.hotReloadTasks: Future<Collection<Provider<ComposeHotReloadTask>>> by projectFuture {
    PluginStage.EagerConfiguration.await()

    forAllJvmCompilations { compilation ->
        compilation.hotReloadTask.await()
    }
}

internal val KotlinCompilation<*>.hotReloadTask: Future<TaskProvider<ComposeHotReloadTask>> by future {
    if (hotReloadTaskName in project.tasks.names) return@future project.tasks.named(name, ComposeHotReloadTask::class.java)

    val task = project.tasks.register(hotReloadTaskName, ComposeHotReloadTask::class.java) { task ->
        task.description = "Hot Reloads code associated with the '$this'"
        task.onlyIf("Running application is known") { task.agentPort.isPresent }
        task.agentPort.set(pidFileOrchestrationPort)
    }

    val snapshotTask = hotSnapshotTask.await()
    task.configure { task ->
        task.pendingRequestFile.set(snapshotTask.flatMap { it.pendingRequestFile })
        task.dependsOn(snapshotTask)
    }

    task
}


@DisableCachingByDefault(because = "Should always run")
@InternalHotReloadApi
abstract class ComposeHotReloadTask : DefaultTask(), ComposeHotReloadOtherTask {
    private val rootProjectDirectory = project.rootProject.layout.projectDirectory

    @get:Internal
    val agentPort: Property<Int> = project.objects.property<Int>()

    @get:Internal
    val pendingRequestFile: RegularFileProperty = project.objects.fileProperty()

    @Inject
    abstract fun getCancellationToken(): BuildCancellationToken

    @TaskAction
    fun execute() {
        OrchestrationClient(Compiler, agentPort.get()).use { client ->
            client.connectBlocking().leftOr {
                logger.quiet("Failed to create 'OrchestrationClient'!")
                getCancellationToken().cancel()
                error("Failed to create 'OrchestrationClient'!")
            }

            logger.quiet("Connected to '${client.port.getOrNull()}'")

            val pendingRequestFile = pendingRequestFile.get().asFile.toPath()

            val request = if (pendingRequestFile.exists()) pendingRequestFile.readObject<ReloadClassesRequest>() else {
                logger.info("UP-TO-DATE")
                ReloadClassesRequest(emptyMap())
            }

            if (request.changedClassFiles.isEmpty()) {
                logger.debug("UP-TO-DATE: No changed classes found")
            }

            logger.quiet(reloadReport(request))
            client.sendBlocking(request)
            pendingRequestFile.deleteIfExists()
        }
    }


    private fun reloadReport(request: ReloadClassesRequest): String {
        val rootPath = rootProjectDirectory.asFile
        val entries = request.changedClassFiles.entries.toTypedArray()

        return """
            Reloading classes
              Application listening at '{{port}}'
              Request ID: '{{messageId}}'
              
              Summary: 
                - Modifications: {{modifications}}
                - Additions: {{additions}}
                - Deletions: {{deletions}}
              
              Details: 
                - {{detail}}
        """.trimIndent().asTemplateOrThrow().renderOrThrow {
            "port"(agentPort.get())
            "messageId"(request.messageId)
            "modifications"(entries.count { it.value == Modified })
            "additions"(entries.count { it.value == Added })
            "deletions"(entries.count { it.value == Removed })

            entries.forEach { (file, changeType) ->
                "detail"("${file.relativeToOrSelf(rootPath)}: ${changeType.name.lowercase()}")
            }
        }
    }
}

@DisableCachingByDefault(because = "Lifecycle task: Not worth caching")
internal abstract class ComposeHotReloadLifecycleTask : DefaultTask(), ComposeHotReloadOtherTask
