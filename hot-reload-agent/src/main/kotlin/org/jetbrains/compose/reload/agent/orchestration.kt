package org.jetbrains.compose.reload.agent

import org.jetbrains.compose.reload.orchestration.OrchestrationClient
import org.jetbrains.compose.reload.orchestration.OrchestrationClientRole.Application
import org.jetbrains.compose.reload.orchestration.OrchestrationHandle
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.startOrchestrationServer
import java.util.concurrent.Future
import kotlin.concurrent.thread

private val logger = createLogger()

fun OrchestrationMessage.send(): Future<Unit> {
    return ComposeHotReloadAgent.orchestration.sendMessage(this)
}

internal fun startOrchestration(): OrchestrationHandle {
    val orchestration = run {
        /* Connecting to a server if we're instructed to */
        OrchestrationClient(Application)?.let { client ->
            logger.debug("Hot Reload Agent is starting in 'client' mode (connected to '${client.port}')")
            return@run client
        }

        /* Otherwise, we start our own orchestration server */
        logger.debug("Hot Reload Agent is starting in 'server' mode")
        startOrchestrationServer()
    }

    Runtime.getRuntime().addShutdownHook(thread(start = false) {
        logger.debug("Hot Reload Agent is shutting down")
        orchestration.closeImmediately()
    })

    return orchestration
}