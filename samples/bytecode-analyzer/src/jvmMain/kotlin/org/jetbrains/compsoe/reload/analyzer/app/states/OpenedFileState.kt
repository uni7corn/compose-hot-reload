package org.jetbrains.compsoe.reload.analyzer.app.states

import io.sellmair.evas.State
import io.sellmair.evas.collectEvents
import io.sellmair.evas.launchState
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compsoe.reload.analyzer.app.events.RegularFileOpenEvent
import java.nio.file.Path

data class OpenedFileState(val path: Path) : State {
    companion object Key : State.Key<OpenedFileState?> {
        override val default: OpenedFileState? = null
    }
}

fun CoroutineScope.launchOpenFileState() = launchState(OpenedFileState.Key) {
    collectEvents<RegularFileOpenEvent> { event ->
        OpenedFileState(event.path).emit()
    }
}