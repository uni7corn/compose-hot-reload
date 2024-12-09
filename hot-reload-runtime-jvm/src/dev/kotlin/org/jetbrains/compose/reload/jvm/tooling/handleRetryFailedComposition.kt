package org.jetbrains.compose.reload.jvm.tooling

import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent
import org.jetbrains.compose.reload.agent.ComposeReloadPremainExtension
import org.jetbrains.compose.reload.agent.orchestration
import org.jetbrains.compose.reload.core.createLogger
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.invokeWhenReceived

private val logger = createLogger()

internal class RetryFailedCompositionHandler : ComposeReloadPremainExtension {
    override fun premain() {
        ComposeHotReloadAgent.orchestration.invokeWhenReceived<OrchestrationMessage.RetryFailedCompositionRequest> {
            retryFailedCompositions()
        }
    }

    companion object {
        fun retryFailedCompositions() {
            logger.orchestration("ErrorRecovery: retryFailedCompositions")
            @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
            androidx.compose.runtime.Recomposer.loadStateAndComposeForHotReload(emptyList<Any>())
        }
    }
}
