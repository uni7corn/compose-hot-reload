package org.jetbrains.compose.reload.jvm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent
import java.util.UUID

internal object HotReloadHooks {
    val hotReloadFlow: Flow<HotReloadState> = run {
        val flow = MutableStateFlow(HotReloadState(null, 0))
        try {
            ComposeHotReloadAgent.invokeAfterReload { reloadRequestId: UUID, error ->
                flow.update { it.copy(reloadRequestId = reloadRequestId, iteration = it.iteration + 1, error = error) }
            }
        } catch (e: LinkageError) {
            //
        }

        flow
    }
}

internal data class HotReloadState(
    val reloadRequestId: UUID? = null,
    val iteration: Int,
    val error: Throwable? = null
)