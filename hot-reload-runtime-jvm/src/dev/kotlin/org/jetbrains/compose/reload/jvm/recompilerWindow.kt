package org.jetbrains.compose.reload.jvm

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.withContext
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent
import org.jetbrains.compose.reload.agent.ComposeReloadPremainExtension
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.LogMessage
import org.jetbrains.compose.reload.orchestration.asFlow
import java.net.URL
import kotlin.concurrent.thread

private val logger = createLogger()

internal class RecompilerWindow : ComposeReloadPremainExtension {
    override fun premain() {
        /*
        On headless mode: Don't show a window
        */
        if (HotReloadEnvironment.isHeadless) {
            return
        }

        /*
        Otherwise, we start a new windows (application) to show gradle
         */
        thread(name = "Compose Recompiler Window") {
            try {
                runRecompilerApplication()
            } catch (_: InterruptedException) {

            }

            logger.debug("Compose Recompiler Window finished")
        }
    }
}

private fun runRecompilerApplication() {
    singleWindowApplication(
        title = "Compose Hot Recompiler",
        state = WindowState(position = WindowPosition.Aligned(Alignment.BottomEnd)),
        exitProcessOnExit = false,
    ) {

        var outputLines by remember { mutableStateOf(listOf<String>()) }

        LaunchedEffect(Unit) {
            ComposeHotReloadAgent.orchestration.asFlow().filterIsInstance<LogMessage>()
                .filter { message -> message.tag == LogMessage.TAG_COMPILER }
                .collect { value -> outputLines = (outputLines + value.log).takeLast(1024) }
        }

        var composeLogo by remember { mutableStateOf<ImageBitmap?>(null) }


        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                runCatching {
                    composeLogo =
                        URL("https://github.com/JetBrains/compose-multiplatform/blob/master/artwork/compose-logo.png?raw=true").openStream()
                            .buffered().use { input -> loadImageBitmap(input) }
                }.onFailure {
                    logger.error("Failed loading compose-logo.png", it)
                }
            }
        }

        Column(Modifier.padding(16.dp)) {
            Row {
                composeLogo?.let { bitmap ->
                    Image(
                        bitmap, "Compose Logo", modifier = Modifier.size(64.dp)
                    )
                }

                Column {
                    Text("Compose Application Recompiler", fontSize = 24.0f.sp, fontWeight = FontWeight.Bold)
                    Text("Save your code to recompile!", fontSize = 16.0f.sp)
                }
            }


            Row {
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
    }
}


internal fun createGradleCommand(projectPath: String, task: String): String {
    if (projectPath == ":") return ":$task"
    return "$projectPath:$task"
}