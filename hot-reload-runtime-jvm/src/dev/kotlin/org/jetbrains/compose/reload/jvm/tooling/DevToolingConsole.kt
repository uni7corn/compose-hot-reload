package org.jetbrains.compose.reload.jvm.tooling

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.LogMessage
import org.jetbrains.compose.reload.orchestration.asFlow

@Composable
fun DevToolingConsole(tag: String, modifier: Modifier) {
    var outputLines by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(Unit) {
        ComposeHotReloadAgent.orchestration.asFlow().filterIsInstance<LogMessage>()
            .filter { message -> message.tag == tag }
            .collect { value -> outputLines = (outputLines + value.message).takeLast(1024) }
    }


    Row(modifier) {
        Card(Modifier.padding(16.dp).fillMaxWidth()) {
            Box(Modifier.padding(16.dp)) {
                val listState = LazyListState(outputLines.lastIndex)

                LazyColumn(state = listState) {
                    items(outputLines) { text ->
                        Row {
                            Text(text, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}