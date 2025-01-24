package org.jetbrains.compose.reload.jvm

import androidx.compose.runtime.Recomposer
import org.jetbrains.compose.reload.agent.orchestration
import org.jetbrains.compose.reload.core.createLogger

private val logger = createLogger()

internal fun retryFailedCompositions() {
    logger.orchestration("ErrorRecovery: retryFailedCompositions")
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    Recomposer.loadStateAndComposeForHotReload(emptyList<Any>())
}
