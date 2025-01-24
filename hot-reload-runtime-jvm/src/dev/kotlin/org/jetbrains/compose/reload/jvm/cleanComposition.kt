package org.jetbrains.compose.reload.jvm

import kotlinx.coroutines.flow.update

internal fun cleanComposition() {
    hotReloadState.update { state -> state.copy(key = state.key + 1) }
    retryFailedCompositions()
}
