package org.jetbrains.compose.reload.jvm.tooling

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import kotlinx.coroutines.delay
import org.jetbrains.compose.reload.jvm.tooling.states.ReloadState
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ApplicationScope.DevToolingSidecar(windowState: WindowState) {
    val animationDuration = 256
    var isExpanded by remember { mutableStateOf(false) }
    var sideCarWidth by remember { mutableStateOf(if (isExpanded) 512.dp else 64.dp) }

    if (isExpanded) {
        sideCarWidth = 512.dp
    }

    if (!isExpanded) {
        LaunchedEffect(Unit) {
            delay(animationDuration.milliseconds)
            sideCarWidth = 64.dp
        }
    }

    val targetX = windowState.position.x - sideCarWidth.value.dp
    val targetY = windowState.position.y

    val xAnimatable = remember { Animatable(targetX.value) }
    val yAnimatable = remember { Animatable(targetY.value) }

    val x by xAnimatable.asState()
    val y by yAnimatable.asState()

    val height by animateDpAsState(targetValue = windowState.size.height)

    LaunchedEffect(windowState.position) {
        xAnimatable.animateTo(targetX.value)
    }

    LaunchedEffect(windowState.position) {
        yAnimatable.animateTo(targetY.value)
    }

    LaunchedEffect(sideCarWidth) {
        xAnimatable.snapTo(targetX.value)
        yAnimatable.snapTo(targetY.value)
    }

    val sidecarWindowState = WindowState(
        width = sideCarWidth, height = height,
        position = WindowPosition(x = x.dp, y = y.dp)
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
                        .padding(4.dp)
                        .then(
                            if (reloadState is ReloadState.Reloading)
                                Modifier.border(2.dp, animatedReloadStateBrush(), RoundedCornerShape(8.dp))
                            else if (reloadState is ReloadState.Failed)
                                Modifier.border(2.dp, reloadStateColor, RoundedCornerShape(8.dp))
                            else if (isExpanded) Modifier.border(2.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            else Modifier
                        )
                        .then(if (!isExpanded) Modifier.padding(4.dp) else Modifier)
                        .clip(RoundedCornerShape(8.dp))
                        .background((if (isExpanded) Color.White else animateReloadStateColor().value).copy(alpha = 0.3f))
                        .background(Color.White.copy(alpha = 0.75f))
                ) {
                    Row(
                        Modifier.align(Alignment.End).padding(4.dp).animateContentSize(tween(animationDuration)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {

                        AnimatedVisibility(
                            isExpanded,
                            enter = fadeIn(tween(animationDuration)) +
                                    scaleIn(tween(animationDuration), initialScale = 0.7f),
                            exit = fadeOut(tween(animationDuration)) +
                                    scaleOut(tween(animationDuration), targetScale = 0.7f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ComposeLogo(modifier = Modifier.size(32.dp))
                                Text("Save your code to recompile!", fontSize = 16.0f.sp)
                            }
                        }
                        if (isExpanded) {
                            Spacer(Modifier.weight(1f))
                        }
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier
                                .padding(2.dp)
                                .size(24.dp)
                        ) {
                            if (isExpanded)
                                Icon(
                                    Icons.Default.Close, contentDescription = "Close",
                                    modifier = Modifier.fillMaxSize()
                                )
                            else ComposeLogo(Modifier.fillMaxSize())
                        }
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn(tween(animationDuration)) +
                                scaleIn(tween(animationDuration), initialScale = 0.7f) +
                                expandIn(expandFrom = Alignment.Center, animationSpec = tween(animationDuration)),
                        exit = fadeOut(tween(animationDuration)) +
                                scaleOut(tween(animationDuration), targetScale = 0.7f) +
                                shrinkOut(shrinkTowards = Alignment.Center, animationSpec = tween(animationDuration))
                    ) {
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
