package org.jetbrains.compose.reload.jvm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent
import org.jetbrains.compose.reload.agent.send
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage

@Composable
internal fun HotReloadErrorWidget(error: Throwable, modifier: Modifier = Modifier.Companion) {
    Box(modifier = modifier.background(Color.Companion.Gray.copy(alpha = 0.95f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Failed reloading code", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.padding(8.dp))
            Text(error.message.orEmpty(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.weight(1f, fill = false),
                elevation = CardDefaults.elevatedCardElevation()
            ) {
                LazyColumn(Modifier.padding(16.dp).background(Color.Black)) {
                    items(error.stackTrace.size) { index ->
                        Text(error.stackTrace[index].toString(), color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation()
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { ComposeHotReloadAgent.retryPendingChanges() },
                        colors = ButtonDefaults.textButtonColors()
                    ) {
                        Text("Retry")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { OrchestrationMessage.ShutdownRequest().send() },
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Text("Shutdown")
                    }
                }
            }
        }
    }
}