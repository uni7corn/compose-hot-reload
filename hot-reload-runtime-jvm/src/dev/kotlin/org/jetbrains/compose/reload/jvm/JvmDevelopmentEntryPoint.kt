@file:JvmName("JvmDevelopmentEntryPoint")

package org.jetbrains.compose.reload.jvm

import androidx.compose.runtime.*
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import org.jetbrains.compose.reload.InternalHotReloadApi
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent
import org.jetbrains.compose.reload.agent.orchestration
import org.jetbrains.compose.reload.agent.send
import org.jetbrains.compose.reload.core.createLogger
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.*
import org.jetbrains.compose.reload.orchestration.asFlow
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.*

private val logger = createLogger()

@Composable
@PublishedApi
@InternalHotReloadApi
internal fun JvmDevelopmentEntryPoint(child: @Composable () -> Unit) {
    /* Checking if we're currently in the stack of a hot reload */
    if (hotReloadStateLocal.current != null) {
        child()
        return
    }

    val windowId = startWindowManager()

    LaunchedEffect(Unit) {
        ComposeHotReloadAgent.orchestration.asFlow().filterIsInstance<CleanCompositionRequest>().collect { value ->
            cleanComposition()
        }
    }

    LaunchedEffect(Unit) {
        ComposeHotReloadAgent.orchestration.asFlow().filterIsInstance<RetryFailedCompositionRequest>().collect {
            retryFailedCompositions()
        }
    }

    /* Agent */
    val currentHotReloadState by hotReloadState.collectAsState()

    CompositionLocalProvider(hotReloadStateLocal provides currentHotReloadState) {
        key(currentHotReloadState.key) {
            logger.orchestration("Composing UI: $currentHotReloadState")
            runCatching { child() }.onFailure { exception ->
                logger.orchestration("Failed invoking 'JvmDevelopmentEntryPoint':", exception)

                /*
                UI-Exception: Nuke state captured in the UI by incrementing the key
                 */
                hotReloadState.update { state -> state.copy(key = state.key + 1, error = exception) }

                UIException(
                    windowId = windowId,
                    message = exception.message,
                    stacktrace = exception.stackTrace.toList()
                ).send()

            }.getOrThrow()
        }
    }


    /* Notify orchestration about the UI being rendered */
    UIRendered(
        windowId = windowId, reloadRequestId = currentHotReloadState.reloadRequestId, currentHotReloadState.iteration
    ).send()
}
