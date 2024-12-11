package org.jetbrains.compose.reload.jvm.tooling

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp
import io.sellmair.evas.compose.composeValue
import kotlinx.coroutines.delay
import org.jetbrains.compose.reload.jvm.tooling.states.ReloadState

private val Orange1 = Color(0xFFFC801D)
private val Orange2 = Color(0xFFFDB60D)
private val Green = Color(0xFF3DEA62)
private val Red = Color(0xFFFE2857)

@Composable
fun animateReloadStateColor(state: ReloadState = ReloadState.composeValue()): State<Color> {
    return animateColorAsState(
        when (state) {
            is ReloadState.Reloading -> Color.Transparent
            is ReloadState.Ok -> Green
            is ReloadState.Failed -> Red
        }
    )
}

@Composable
fun animatedReloadStateBrush() : Brush {
    val infiniteTransition = rememberInfiniteTransition()
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing))
    )

    return Brush.linearGradient(
        colors = listOf(Orange1, Orange2),
        start = Offset(0f, gradientShift),
        end = Offset(0f, gradientShift + 400),
        tileMode = TileMode.Mirror,
    )
}

@Composable
fun ReloadStateBanner(state: ReloadState, modifier: Modifier = Modifier) {
    Box(modifier = modifier.width(8.dp)) {

        val color by animateReloadStateColor(state)

        val visibilityState = remember { MutableTransitionState(false) }
        LaunchedEffect(state) {
            if (state is ReloadState.Ok) {
                delay(1000)
                visibilityState.targetState = false
            } else {
                visibilityState.targetState = true
            }
        }

        AnimatedVisibility(
            visibleState = visibilityState,
            enter = slideInHorizontally(
                animationSpec = tween(50),
                initialOffsetX = { it }
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(200),
                targetOffsetX = { it }
            ),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .wrapContentWidth()
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(animatedReloadStateBrush())
                    .background(color)
            )
        }
    }
}
