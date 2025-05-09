/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.compose.devtools.widgets

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import org.jetbrains.compose.devtools.theme.DtColors

@Immutable
data class DtButtonState(
    val isHovered: Boolean,
    val isPressed: Boolean,
)

@Composable
fun DtButton(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable (state: DtButtonState) -> Unit = { _ -> },
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHoveredScale = remember { Animatable(1f) }
    val isPressedScale = remember { Animatable(1f) }

    val color = remember { Animatable(DtColors.surface) }

    LaunchedEffect(isHovered) {
        isHoveredScale.animateTo(if (isHovered) .975f else 1f)
    }

    LaunchedEffect(isHovered) {
        color.animateTo(if(isHovered) DtColors.surfaceActive else DtColors.surface)
    }

    LaunchedEffect(Unit) {
        interactionSource.interactions.filterIsInstance<PressInteraction>().collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isPressedScale.animateTo(1.05f)
                is PressInteraction.Cancel -> isPressedScale.animateTo(1f, tween(100))
                is PressInteraction.Release -> isPressedScale.animateTo(1f, tween(256))
            }
        }
    }

    Surface(
        modifier = modifier.hoverable(interactionSource)
            .scale(isHoveredScale.value)
            .scale(isPressedScale.value)
            .hoverable(interactionSource),
        shadowElevation = 2.dp,
        color = color.value,
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(
            modifier = Modifier
                .clickable(interactionSource, indication = ripple(), onClick = onClick)
        ) {
            content(DtButtonState(isHovered = isHovered, isPressed = isPressed))
        }
    }
}
