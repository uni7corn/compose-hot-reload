package org.jetbrains.compose.reload.jvm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent
import org.jetbrains.compose.reload.agent.ComposeReloadPremainExtension

private val logger = createLogger()

internal class ErrorRecovery : ComposeReloadPremainExtension {
    override fun premain() {
        ComposeHotReloadAgent.invokeAfterReload { _, error ->
            if (error != null) return@invokeAfterReload

            runBlocking(Dispatchers.Main) {
                retryFailedCompositions()
            }
        }
    }

    fun retryFailedCompositions() {
        logger.info("ErrorRecovery: retryFailedCompositions")
        @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
        androidx.compose.runtime.Recomposer.loadStateAndComposeForHotReload(emptyList<Any>())
    }
}
