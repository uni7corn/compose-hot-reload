package org.jetbrains.compose.reload.jvm.tooling

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent
import org.jetbrains.compose.reload.agent.ComposeReloadPremainExtension
import org.jetbrains.compose.reload.agent.orchestration
import org.jetbrains.compose.reload.core.createLogger
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.RetryFailedCompositionRequest
import org.jetbrains.compose.reload.orchestration.asFlow

private val logger = createLogger()

internal class RetryFailedCompositionHandler : ComposeReloadPremainExtension {
    override fun premain() {
        devToolingScope.launch {
            ComposeHotReloadAgent.orchestration.asFlow().filterIsInstance<RetryFailedCompositionRequest>().collect {
                retryFailedCompositions()
            }
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
