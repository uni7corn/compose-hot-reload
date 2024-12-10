@file:OptIn(ExperimentalComposeUiApi::class)

package org.jetbrains.compose.reload.jvm.tooling

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ApplicationScope
import io.sellmair.evas.compose.composeValue
import org.jetbrains.compose.reload.jvm.tooling.states.WindowsState

@Composable
fun ApplicationScope.DevOverlays() {
    val windows = WindowsState.composeValue().windows
    windows.forEach { (windowId, windowState) ->
        DevToolingSidebar(windowState)
        DevToolingErrorOverlay(windowId, windowState)
    }
}
