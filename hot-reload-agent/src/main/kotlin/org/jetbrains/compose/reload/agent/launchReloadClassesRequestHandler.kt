package org.jetbrains.compose.reload.agent

import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.invokeWhenReceived
import java.io.File
import java.lang.instrument.Instrumentation
import kotlin.concurrent.withLock
import kotlin.system.exitProcess

internal fun launchReloadClassesRequestHandler(instrumentation: Instrumentation) {
    var pendingChanges = mapOf<File, OrchestrationMessage.ReloadClassesRequest.ChangeType>()

    ComposeHotReloadAgent.orchestration.invokeWhenReceived<OrchestrationMessage.ReloadClassesRequest> { request ->
        ComposeHotReloadAgent.reloadLock.withLock {
            pendingChanges = pendingChanges + request.changedClassFiles

            ComposeHotReloadAgent.executeBeforeReloadListeners(request.messageId)
            val result = runCatching { reload(instrumentation, pendingChanges) }

            /*
            Yuhuu! We reloaded the classes; We can reset the 'pending changes'; No re-try necessary
             */
            if (result.isSuccess) {
                pendingChanges = emptyMap()
            }

            if (result.isFailure) {
                logger.warn("Failed to reload classes", result.exceptionOrNull())
            }

            ComposeHotReloadAgent.executeAfterReloadListeners(request.messageId, result.exceptionOrNull())
        }
    }

    ComposeHotReloadAgent.orchestration.invokeWhenReceived<OrchestrationMessage.ShutdownRequest> {
        logger.info("Received shutdown request")
        exitProcess(0)
    }

    ComposeHotReloadAgent.orchestration.invokeWhenClosed {
        logger.info("Application Orchestration closed")
        exitProcess(0)
    }
}