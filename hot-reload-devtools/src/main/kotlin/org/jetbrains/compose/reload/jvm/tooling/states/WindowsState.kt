package org.jetbrains.compose.reload.jvm.tooling.states

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import io.sellmair.evas.State
import io.sellmair.evas.launchState
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.reload.core.WindowId
import org.jetbrains.compose.reload.jvm.tooling.orchestration
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.asFlow

data class WindowsState(
    val windows: Map<WindowId, WindowState>
) : State {

    companion object Key : State.Key<WindowsState> {
        override val default: WindowsState = WindowsState(windows = emptyMap())
    }
}

fun CoroutineScope.launchWindowsState() = launchState(WindowsState.Key) {
    val windows = mutableMapOf<WindowId, WindowState>()

    suspend fun update() {
        WindowsState(windows = windows.toMap()).emit()
    }

    orchestration.asFlow().collect { message ->
        if (message is OrchestrationMessage.ApplicationWindowPositioned) {
            windows[message.windowId] = WindowState(
                placement = WindowPlacement.Floating,
                position = WindowPosition(message.x.dp, message.y.dp),
                size = DpSize(message.width.dp, message.height.dp)
            )
            update()
        }

        if (message is OrchestrationMessage.ApplicationWindowGone) {
            windows.remove(message.windowId)
            update()
        }
    }
}
