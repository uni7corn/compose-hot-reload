package org.jetbrains.compose.reload.jvm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent
import org.jetbrains.compose.reload.agent.send
import org.jetbrains.compose.reload.core.WindowId
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ApplicationWindowPositioned
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ClientConnected
import org.jetbrains.compose.reload.orchestration.asFlow
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.*

private val windowsIds = WeakHashMap<Window, WindowId>()

@Composable
@Suppress("INVISIBLE_REFERENCE")
internal fun startWindowManager(): WindowId? {
    val window = androidx.compose.ui.window.LocalWindow.current
    val windowId = if (window != null) windowsIds.getOrPut(window) { WindowId.create() } else null

    LaunchedEffect(windowId) {
        if (window == null || windowId == null) return@LaunchedEffect

        fun broadcastWindowPosition() {
            ApplicationWindowPositioned(windowId, window.x, window.y, window.width, window.height).send()
        }

        broadcastWindowPosition()

        val windowListener = object : WindowAdapter() {
            override fun windowIconified(e: WindowEvent?) {
                OrchestrationMessage.ApplicationWindowGone(windowId).send()
            }

            override fun windowDeiconified(e: WindowEvent?) {
                broadcastWindowPosition()
            }

            override fun windowClosed(e: WindowEvent?) {
                OrchestrationMessage.ApplicationWindowGone(windowId).send()
            }
        }

        val componentListener = object : ComponentAdapter() {
            override fun componentHidden(e: ComponentEvent?) {
                OrchestrationMessage.ApplicationWindowGone(windowId).send()
            }

            override fun componentShown(e: ComponentEvent?) {
                broadcastWindowPosition()
            }

            override fun componentResized(e: ComponentEvent?) {
                broadcastWindowPosition()
            }

            override fun componentMoved(e: ComponentEvent?) {
                broadcastWindowPosition()
            }
        }

        window.addWindowListener(windowListener)
        window.addComponentListener(componentListener)

        launch {
            ComposeHotReloadAgent.orchestration.asFlow().filterIsInstance<ClientConnected>().collect { message ->
                broadcastWindowPosition()
            }
        }

        currentCoroutineContext().job.invokeOnCompletion {
            window.removeWindowListener(windowListener)
            window.removeComponentListener(componentListener)
        }

        awaitCancellation()
    }

    return windowId
}
