/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.compose.reload.jvm.tooling.sidecar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.sellmair.evas.compose.composeValue
import org.jetbrains.compose.reload.jvm.tooling.states.ConsoleLogState
import org.jetbrains.compose.reload.jvm.tooling.theme.DtColors
import org.jetbrains.compose.reload.jvm.tooling.theme.dtHorizontalPadding
import org.jetbrains.compose.reload.jvm.tooling.theme.dtVerticalPadding
import org.jetbrains.compose.reload.jvm.tooling.widgets.DtCode
import org.jetbrains.compose.reload.jvm.tooling.widgets.DtHeader2
import org.jetbrains.compose.reload.jvm.tooling.widgets.animatedReloadStatusBorder
import org.jetbrains.compose.reload.jvm.tooling.widgets.animatedReloadStatusBrush

@Composable
fun DtConsole(tag: String, modifier: Modifier = Modifier) {
    val logState = ConsoleLogState.Key(tag).composeValue()
    val scroll = rememberScrollState(0)

    LaunchedEffect(scroll.maxValue) {
        if (logState.logs.isEmpty()) return@LaunchedEffect
        scroll.scrollTo(scroll.maxValue)
    }

    Column(modifier = modifier) {
        DtHeader2(tag, modifier = Modifier.dtHorizontalPadding())
        Box(
            Modifier
                .dtHorizontalPadding()
                .dtVerticalPadding()
                .animatedReloadStatusBorder()
                .background(DtColors.surfaceConsole)
                .fillMaxSize()
        ) {
            SelectionContainer(modifier= Modifier.dtVerticalPadding()) {
                DtCode(
                    logState.logs.joinToString("\n"),
                    modifier = Modifier
                        .verticalScroll(scroll)
                        .dtHorizontalPadding()
                        .dtVerticalPadding()
                )
            }
        }
    }
}
