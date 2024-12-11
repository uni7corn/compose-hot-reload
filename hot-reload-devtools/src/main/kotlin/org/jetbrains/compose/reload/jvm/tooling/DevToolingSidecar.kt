package org.jetbrains.compose.reload.jvm.tooling

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import io.sellmair.evas.compose.composeState
import io.sellmair.evas.compose.composeValue
import org.jetbrains.compose.reload.jvm.tooling.states.ReloadState

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
        var isExpanded by remember { mutableStateOf(false) }
        val reloadState by ReloadState.composeState()
        val reloadStateColor by animateReloadStateColor(reloadState)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.End,
        ) {
            Row {
                Column(
                    modifier = Modifier
                        .heightIn(max = windowState.size.height)
                        .widthIn(max = sidecarWidth)
                        .padding(4.dp)
                        .then(
                            if (reloadState is ReloadState.Reloading)
                                Modifier.border(2.dp, animatedReloadStateBrush(), RoundedCornerShape(8.dp))
                            else if (reloadState is ReloadState.Failed)
                                Modifier.border(2.dp, reloadStateColor, RoundedCornerShape(8.dp))
                            else if (isExpanded) Modifier.border(2.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            else Modifier
                        )
                        .then(if(!isExpanded) Modifier.padding(4.dp) else Modifier)
                        .clip(RoundedCornerShape(8.dp))
                        .background((if (isExpanded) Color.White else animateReloadStateColor().value).copy(alpha = 0.3f))
                        .background(Color.White.copy(alpha = 0.75f))
                ) {
                    Row(
                        Modifier.align(Alignment.End).padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isExpanded) {
                            ComposeLogo(modifier = Modifier.size(32.dp))
                            Text("Save your code to recompile!", fontSize = 16.0f.sp)
                            Spacer(Modifier.weight(1f))
                        }
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier
                                .padding(2.dp)
                                .size(24.dp)
                        ) {
                            if (isExpanded) Icon(Icons.Default.Close, contentDescription = "Close")
                            else ComposeLogo(Modifier.fillMaxSize())
                        }
                    }


                    if (isExpanded) {
                        DevToolingWidget(Modifier.padding(8.dp).fillMaxSize())
                    }

                }

                ReloadStateBanner(
                    ReloadState.composeValue(),
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }
}
