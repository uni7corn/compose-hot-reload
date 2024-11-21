package org.jetbrains.compose.reload.jvm.tooling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.reload.agent.send
import org.jetbrains.compose.reload.jvm.hotReloadState
import org.jetbrains.compose.reload.jvm.retryFailedCompositions
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage

@Composable
fun DevToolingToolbar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(128.dp).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = {
            OrchestrationMessage.ShutdownRequest().send()
        }) {
            Text("Exit")
        }

        Button(onClick = {
            retryFailedCompositions()
        }) {
            Text("Retry failed compositions")
        }

        Button(onClick = {
            hotReloadState.update { state -> state.copy(key = state.key + 1) }
            retryFailedCompositions()
        }) {
            Text("Clean composition")
        }
    }
}