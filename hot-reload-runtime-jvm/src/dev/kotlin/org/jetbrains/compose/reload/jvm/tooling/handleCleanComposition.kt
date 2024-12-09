package org.jetbrains.compose.reload.jvm.tooling

import kotlinx.coroutines.flow.update
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent
import org.jetbrains.compose.reload.agent.ComposeReloadPremainExtension
import org.jetbrains.compose.reload.jvm.hotReloadState
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.invokeWhenReceived

internal class CleanCompositionHandler : ComposeReloadPremainExtension {
    override fun premain() {
        ComposeHotReloadAgent.orchestration.invokeWhenReceived<OrchestrationMessage.CleanCompositionRequest> {
            cleanComposition()
        }
    }

    companion object {
        fun cleanComposition() {
            hotReloadState.update { state -> state.copy(key = state.key + 1) }
            RetryFailedCompositionHandler.retryFailedCompositions()
        }
    }
}
