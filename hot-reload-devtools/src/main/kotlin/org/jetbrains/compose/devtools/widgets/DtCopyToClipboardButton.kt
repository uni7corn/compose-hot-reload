/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.compose.devtools.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import org.jetbrains.compose.devtools.sidecar.dtBackground
import org.jetbrains.compose.devtools.theme.DtPadding
import org.jetbrains.compose.devtools.theme.DtTextStyles
import java.awt.datatransfer.StringSelection


@Composable
fun DtCopyToClipboardButton(
    text: String,
    content: () -> String,
) {
    val clipboard = LocalClipboard.current
    var copyAll by remember { mutableStateOf(false) }
    LaunchedEffect(copyAll) {
        if (copyAll) {
            clipboard.setClipEntry(ClipEntry(StringSelection(content())))
            copyAll = false
        }
    }

    BasicText(
        text = text,
        style = DtTextStyles.small,
        modifier = Modifier
            .dtBackground()
            .padding(horizontal = DtPadding.medium, vertical = DtPadding.small)
            .clickable { copyAll = true }
    )
}
