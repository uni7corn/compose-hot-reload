@file:JvmName("JvmDevelopmentEntryPoint")

package org.jetbrains.compose.reload.jvm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.reflect.ComposableMethod
import androidx.compose.runtime.reflect.asComposableMethod
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.reload.InternalHotReloadApi
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent
import org.jetbrains.compose.reload.agent.send
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import java.util.UUID
import kotlin.concurrent.withLock

private val logger = createLogger()

private data class HotReloadState(
    val reloadRequestId: UUID? = null,
    val iteration: Int,
    val error: Throwable? = null
)

private val hotReloadStateLocal = staticCompositionLocalOf<HotReloadState?> { null }

private val hotReloadStateFlow: StateFlow<HotReloadState> = MutableStateFlow(HotReloadState(null, 0)).apply {
    ComposeHotReloadAgent.invokeAfterReload { reloadRequestId: UUID, error ->
        update { it.copy(reloadRequestId = reloadRequestId, iteration = it.iteration + 1, error = error) }
    }
}

@Composable
@PublishedApi
@InternalHotReloadApi
internal fun JvmDevelopmentEntryPoint(child: @Composable () -> Unit) {
    /* Checking if we're currently in the stack of a hot reload */
    if (hotReloadStateLocal.current != null) {
        child()
        return
    }


    /* Wrap the child to get access to exceptions, which we can forward into the orchestration */
    val interceptedChild: @Composable () -> Unit = {
        runCatching { child() }.onFailure { exception ->
            logger.error("Failed invoking 'JvmDevelopmentEntryPoint':", exception)

            OrchestrationMessage.UIException(
                message = exception.message,
                stacktrace = exception.stackTrace.toList()
            ).send()

        }.getOrThrow()
    }

    /* Agent */
    val hotReloadState by hotReloadStateFlow.collectAsState()

    ComposeHotReloadAgent.reloadLock.withLock {
        CompositionLocalProvider(hotReloadStateLocal provides hotReloadState) {
            logger.debug("Hotswap version: ${hotReloadState.iteration}")

            /* Show hot reload error directly in the UI (and offer retry button) */
            val hotReloadError = hotReloadState.error
            if (hotReloadError != null) {
                Box(Modifier.fillMaxSize()) {
                    interceptedChild()
                    HotReloadErrorWidget(hotReloadError, modifier = Modifier.matchParentSize())
                }
            } else {
                /* If no error is present, we just show the child directly (w/o box) */
                interceptedChild()
            }
        }
    }

    /* Notify orchestration about the UI being rendered */
    OrchestrationMessage.UIRendered(hotReloadState.reloadRequestId, hotReloadState.iteration).send()
}
