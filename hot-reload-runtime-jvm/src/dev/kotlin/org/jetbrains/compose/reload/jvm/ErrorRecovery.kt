package org.jetbrains.compose.reload.jvm

import org.jetbrains.compose.reload.agent.orchestration

private val logger = createLogger()

fun retryFailedCompositions() {
    logger.orchestration("ErrorRecovery: retryFailedCompositions")
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    androidx.compose.runtime.Recomposer.loadStateAndComposeForHotReload(emptyList<Any>())
}
