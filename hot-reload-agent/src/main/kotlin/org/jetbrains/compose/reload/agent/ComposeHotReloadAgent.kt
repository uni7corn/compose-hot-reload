package org.jetbrains.compose.reload.agent

import javassist.ClassPool
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent.reloadLock
import org.jetbrains.compose.reload.orchestration.*
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ReloadClassesRequest
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ReloadClassesRequest.ChangeType
import java.io.File
import java.lang.instrument.ClassDefinition
import java.lang.instrument.Instrumentation
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.system.exitProcess

private val logger = createLogger()


object ComposeHotReloadAgent {

    val reloadLock = ReentrantLock()

    private val beforeReloadListeners = mutableListOf<(reloadRequestId: UUID) -> Unit>()
    private val afterReloadListeners = mutableListOf<(reloadRequestId: UUID, error: Throwable?) -> Unit>()


    @Volatile
    private var instrumentation: Instrumentation? = null

    val orchestration by lazy { startOrchestration() }

    fun invokeBeforeReload(block: (reloadRequestId: UUID) -> Unit) = reloadLock.withLock {
        beforeReloadListeners.add(block)
    }

    fun invokeAfterReload(block: (reloadRequestId: UUID, error: Throwable?) -> Unit) = reloadLock.withLock {
        afterReloadListeners.add(block)
    }

    internal fun executeBeforeReloadListeners(reloadRequestId: UUID) = reloadLock.withLock {
        beforeReloadListeners.forEach { it(reloadRequestId) }
    }

    internal fun executeAfterReloadListeners(reloadRequestId: UUID, error: Throwable?) = reloadLock.withLock {
        afterReloadListeners.forEach { it(reloadRequestId, error) }
    }

    fun retryPendingChanges() {
        orchestration.sendMessage(ReloadClassesRequest())
    }

    @JvmStatic
    fun premain(args: String?, instrumentation: Instrumentation) {
        this.instrumentation = instrumentation
        enableComposeHotReloadMode()
        launchReloadClassesRequestHandler(instrumentation)
    }
}

private fun startOrchestration(): OrchestrationHandle {
    /* Connecting to a server if we're instructed to */
    OrchestrationClient()?.let { client ->
        logger.debug("Hot Reload Agent is starting in 'client' mode (connected to '${client.port}')")
        return client
    }

    /* Otherwise, we start our own orchestration server */
    logger.debug("Hot Reload Agent is starting in 'server' mode")
    return startOrchestrationServer()
}

private fun launchReloadClassesRequestHandler(instrumentation: Instrumentation) {
    var pendingChanges = mapOf<File, ChangeType>()

    ComposeHotReloadAgent.orchestration.invokeWhenReceived<ReloadClassesRequest> { request ->
        reloadLock.withLock {
            pendingChanges = pendingChanges + request.changedClassFiles

            ComposeHotReloadAgent.executeBeforeReloadListeners(request.messageId)
            val result = runCatching { reload(instrumentation, pendingChanges) }

            /*
            Yuhuu! We reloaded the classes; We can reset the 'pending changes'; No re-try necessary
             */
            if (result.isSuccess) {
                pendingChanges = emptyMap()
                resetComposeErrors()
            }

            ComposeHotReloadAgent.executeAfterReloadListeners(request.messageId, result.exceptionOrNull())
        }
    }

    ComposeHotReloadAgent.orchestration.invokeWhenReceived<OrchestrationMessage.ShutdownRequest> {
        exitProcess(0)
    }

    ComposeHotReloadAgent.orchestration.invokeWhenClosed {
        exitProcess(0)
    }
}

private fun reload(
    instrumentation: Instrumentation, pendingChanges: Map<File, ChangeType>
) = reloadLock.withLock {
    val definitions = pendingChanges.mapNotNull { (file, change) ->
        if (change == ChangeType.Removed) {
            return@mapNotNull null
        }

        if (file.extension != "class") {
            logger.warn("$change: $file is not a class")
            return@mapNotNull null
        }

        if (!file.isFile) {
            logger.warn("$change: $file is not a regular file")
            return@mapNotNull null
        }

        logger.debug("Loading: $file")
        val code = file.readBytes()
        val clazz = ClassPool.getDefault().makeClass(code.inputStream())
        ClassDefinition(Class.forName(clazz.name), code)
    }

    instrumentation.redefineClasses(*definitions.toTypedArray())
}
