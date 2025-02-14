/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.compose.reload.jvm.tooling.widgets

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.reload.jvm.tooling.theme.DtTextStyles


@Composable
fun DtHeader1(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text, modifier,
        style = DtTextStyles.header1
    )
}

@Composable
fun DtHeader2(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text, modifier,
        style = DtTextStyles.header2
    )
}

@Composable
fun DtText(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text, modifier,
        style = DtTextStyles.default
    )
}

@Composable
fun DtSmallText(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text, modifier,
        style = DtTextStyles.small
    )
}
