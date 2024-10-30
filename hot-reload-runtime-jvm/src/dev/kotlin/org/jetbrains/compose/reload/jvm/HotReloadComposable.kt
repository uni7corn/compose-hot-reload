package org.jetbrains.compose.reload.jvm


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent
import org.jetbrains.compose.reload.agent.send
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import kotlin.concurrent.withLock

private val logger = createLogger()

private val hotReloadStateLocal = staticCompositionLocalOf<HotReloadState?> { null }

@Composable
internal fun HotReloadComposable(child: @Composable () -> Unit) {
    LaunchedEffect(Unit) {
        composeRecompilerApplication()
    }

    /* Agent */
    val hotReloadState by HotReloadHooks.hotReloadFlow.collectAsState(null)

    ComposeHotReloadAgent.reloadLock.withLock {
        CompositionLocalProvider(hotReloadStateLocal provides hotReloadState) {
            logger.debug("Hotswap version: ${hotReloadState?.iteration}")

            /* Show hot reload error directly in the UI (and offer retry button) */
            val hotReloadError = hotReloadState?.error
            if (hotReloadError != null) {
                Box(Modifier.fillMaxSize()) {
                    child()
                    HotReloadErrorWidget(hotReloadError, modifier = Modifier.matchParentSize())
                }
            } else {
                /* If no error is present, we just show the child directly (w/o box) */
                child()
            }
        }
    }

    /* Notify orchestration about the UI being rendered */
    hotReloadState?.let { hotReloadState ->
        OrchestrationMessage.UIRendered(hotReloadState.reloadRequestId, hotReloadState.iteration).send()
    }
}

