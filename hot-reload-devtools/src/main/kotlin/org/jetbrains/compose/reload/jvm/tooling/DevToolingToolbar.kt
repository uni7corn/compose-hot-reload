package org.jetbrains.compose.reload.jvm.tooling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DevToolingToolbar(modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = {
            OrchestrationMessage.ShutdownRequest().send()
        }) {
            Text("Exit", fontSize = 12.sp)
        }

        Button(onClick = {
            OrchestrationMessage.RetryFailedCompositionRequest().send()
        }) {
            Text("Retry failed compositions", fontSize = 12.sp)
        }

        Button(onClick = {
            OrchestrationMessage.CleanCompositionRequest().send()
        }) {
            Text("Clean composition", fontSize = 12.sp)
        }
    }
}
