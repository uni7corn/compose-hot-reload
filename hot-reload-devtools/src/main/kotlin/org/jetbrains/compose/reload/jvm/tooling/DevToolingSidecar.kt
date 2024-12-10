package org.jetbrains.compose.reload.jvm.tooling

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.LogMessage

@Composable
fun ApplicationScope.DevToolingSidebar(windowState: WindowState) {
    val sidecarWidth = 512.dp

    val x by animateDpAsState(targetValue = windowState.position.x - sidecarWidth)
    val y by animateDpAsState(targetValue = windowState.position.y)
    val height by animateDpAsState(targetValue = windowState.size.height)

    val sidecarWindowState = WindowState(
        width = sidecarWidth, height = height,
        position = WindowPosition(x = x, y = y)
    )

    Window(
        onCloseRequest = {},
        state = sidecarWindowState,
        undecorated = true,
        transparent = true,
        resizable = false,
        focusable = true,
        alwaysOnTop = true
    ) {
        var isShown by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.End,
        ) {

            Column(
                modifier = Modifier
                    .heightIn(max = windowState.size.height)
                    .widthIn(max = sidecarWidth)
                    .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.85f))
            ) {
                Row(
                    Modifier.align(Alignment.End).padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    if (isShown) {
                        ComposeLogo(modifier = Modifier.size(32.dp))
                        Text("Save your code to recompile!", fontSize = 16.0f.sp)
                        Spacer(Modifier.weight(1f))
                    }
                    IconButton(
                        onClick = { isShown = !isShown },
                        modifier = Modifier
                            .padding(4.dp)
                            .size(24.dp)
                    ) {
                        if (isShown) Icon(Icons.Default.Close, contentDescription = "Close")
                        else Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Open")
                    }
                }


                if (!isShown) return@Column

                Column(Modifier.padding(8.dp).fillMaxSize()) {
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
        }
    }
}
