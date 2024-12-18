package org.jetbrains.compose.reload.jvm.tooling

import org.jetbrains.compose.reload.orchestration.OrchestrationClient
import org.jetbrains.compose.reload.orchestration.OrchestrationClientRole
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import java.util.concurrent.Future
import kotlin.system.exitProcess

internal val orchestration = run {
    val client = OrchestrationClient(OrchestrationClientRole.Unknown) ?: error("Failed to create OrchestrationClient")
    client.invokeWhenClosed { exitProcess(0) }
    client
}

internal fun OrchestrationMessage.send(): Future<Unit> {
    return orchestration.sendMessage(this)
}
