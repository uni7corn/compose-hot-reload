package org.jetbrains.compose.reload.jvm.tooling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.LogMessage

@Composable
internal fun DevToolingWidget(modifier: Modifier = Modifier) {
    Column(modifier) {
        DevToolingToolbar()

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            DevToolingConsole(
                tag = LogMessage.TAG_COMPILER,
                modifier = Modifier.weight(1f, fill = false)
            )

            DevToolingConsole(
                tag = LogMessage.TAG_AGENT,
                modifier = Modifier.weight(1f, fill = false),
            )

            DevToolingConsole(
                tag = LogMessage.TAG_RUNTIME,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}
