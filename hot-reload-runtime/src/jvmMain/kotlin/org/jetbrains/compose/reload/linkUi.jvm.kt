package org.jetbrains.compose.reload


import androidx.compose.runtime.*

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
        child()
    }
}

internal val hotReloadStateLocal = staticCompositionLocalOf<HotReloadState?> { null }