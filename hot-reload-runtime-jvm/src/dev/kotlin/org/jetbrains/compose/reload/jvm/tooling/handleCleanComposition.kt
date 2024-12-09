package org.jetbrains.compose.reload.jvm.tooling

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent
import org.jetbrains.compose.reload.agent.ComposeReloadPremainExtension
import org.jetbrains.compose.reload.jvm.hotReloadState
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.CleanCompositionRequest
import org.jetbrains.compose.reload.orchestration.asFlow

internal class CleanCompositionHandler : ComposeReloadPremainExtension {
    override fun premain() {
        devToolingScope.launch {
            ComposeHotReloadAgent.orchestration.asFlow().filterIsInstance<CleanCompositionRequest>().collect { value ->
                cleanComposition()
            }
        }
    }

    companion object {
        fun cleanComposition() {
            hotReloadState.update { state -> state.copy(key = state.key + 1) }
            RetryFailedCompositionHandler.retryFailedCompositions()
        }
    }
}
