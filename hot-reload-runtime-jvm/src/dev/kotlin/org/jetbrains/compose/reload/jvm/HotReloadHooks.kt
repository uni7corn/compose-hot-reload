package org.jetbrains.compose.reload.jvm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent

internal object HotReloadHooks {
    val hotReloadFlow: Flow<HotReloadState> = run {
        val flow = MutableStateFlow(HotReloadState(0))
        try {
            ComposeHotReloadAgent.invokeAfterReload { error ->
                flow.update { it.copy(iteration = it.iteration + 1, error = error) }
            }
        } catch (e: LinkageError) {
            //
        }

        flow
    }
}

internal val reloadLock = try {
    ComposeHotReloadAgent.reloadLock
} catch (e: LinkageError) {
    null
}

internal inline fun withReloadLock(block: () -> Unit) {
    reloadLock?.lock() ?: block()
}

internal data class HotReloadState(
    val iteration: Int,
    val error: Throwable? = null
)