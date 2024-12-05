package org.jetbrains.compsoe.reload.analyzer.app.states

import io.sellmair.evas.State
import io.sellmair.evas.launchState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.jetbrains.compose.reload.analysis.RuntimeInfo
import org.jetbrains.compose.reload.analysis.render
import java.nio.file.Path
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

sealed class RuntimeInfoState : State {

    data object Loading : RuntimeInfoState()
    data class Error(val reason: Throwable?, val message: String? = reason?.message) : RuntimeInfoState()
    data class Result(val info: RuntimeInfo, val rendered: String) : RuntimeInfoState()

    data class Key(val path: Path) : State.Key<RuntimeInfoState?> {
        override val default: RuntimeInfoState? = null
    }
}

fun CoroutineScope.launchRuntimeInfoState() = launchState(
    coroutineContext = Dispatchers.IO,
    keepActive = 1.minutes
) { key: RuntimeInfoState.Key ->
    RuntimeInfoState.Loading.emit()

    while (true) {
        runCatching {
            val bytes = key.path.toFile().readBytes()
            val info = RuntimeInfo(bytes) ?: run {
                RuntimeInfoState.Error(null, "Failed to parse runtime info").emit()
                return@runCatching
            }

            RuntimeInfoState.Result(info, info.render()).emit()
        }.onFailure { exception ->
            RuntimeInfoState.Error(exception).emit()
        }

        delay(5.seconds)
    }
}