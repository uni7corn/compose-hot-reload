/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.compose.reload.jvm.tooling.states

import io.sellmair.evas.State
import io.sellmair.evas.flow
import io.sellmair.evas.launchState
import io.sellmair.evas.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.distinctUntilChanged

data class ReloadCountState(
    val successfulReloads: Int = 0,
    val failedReloads: Int = 0
) : State {
    companion object Key : State.Key<ReloadCountState> {
        override val default: ReloadCountState = ReloadCountState()
    }
}


internal fun CoroutineScope.launchReloadCountState() = launchState(ReloadCountState.Key) {
    ReloadState.flow().buffer().distinctUntilChanged().collect { reloadState ->
        when (reloadState) {
            is ReloadState.Ok -> ReloadCountState.update { count ->
                count.copy(successfulReloads = count.successfulReloads + 1)
            }
            is ReloadState.Failed -> ReloadCountState.update { count ->
                count.copy(failedReloads = count.failedReloads + 1)
            }
            else -> return@collect
        }
    }
}
