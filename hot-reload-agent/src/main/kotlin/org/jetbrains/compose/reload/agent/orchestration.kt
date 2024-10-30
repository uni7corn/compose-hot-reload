package org.jetbrains.compose.reload.agent

import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import java.util.concurrent.Future

fun OrchestrationMessage.send(): Future<Unit> {
    return ComposeHotReloadAgent.orchestration.sendMessage(this)
}