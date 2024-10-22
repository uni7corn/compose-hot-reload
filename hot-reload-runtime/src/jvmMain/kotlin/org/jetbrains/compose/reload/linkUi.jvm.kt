package org.jetbrains.compose.reload


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent

private val logger = createLogger()

@Composable
public fun HotReload(child: @Composable () -> Unit) {
    LaunchedEffect(Unit) {
        composeRecompilerApplication()
    }

    /* Agent */
    val hotReloadState by HotReloadHooks.hotReloadFlow.collectAsState(null)
    CompositionLocalProvider(hotReloadStateLocal provides hotReloadState) {
        logger.debug("Hotswap version: ${hotReloadState?.iteration}")

        /* Show hot reload error directly in the UI (and offer retry button) */
        val hotReloadError = hotReloadState?.error
        if (hotReloadError != null) {
            Box {
                child()
                HotReloadError(hotReloadError, modifier = Modifier.matchParentSize())
            }
        } else {
            /* If no error is present, we just show the child directly (w/o box) */
            child()
        }
    }
}

@Composable
internal fun HotReloadError(error: Throwable, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color.Gray.copy(alpha = 0.9f))) {
        Column {
            Text("Failed reloading code: ${error.message}", color = Color.White)
            Button(onClick = {
                ComposeHotReloadAgent.retryPendingChanges()
            }) {
                Text("Retry")
            }
        }
    }
}

internal val hotReloadStateLocal = staticCompositionLocalOf<HotReloadState?> { null }