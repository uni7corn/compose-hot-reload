/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.compose.reload.jvm.tooling.sidecar

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.reload.jvm.tooling.widgets.DtButton
import org.jetbrains.compose.reload.jvm.tooling.widgets.DtComposeLogo
import org.jetbrains.compose.reload.jvm.tooling.widgets.DtHeader1

@Composable
internal fun DtSidecarHeaderBar(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Companion.CenterVertically,
    ) {
        DtComposeLogo(modifier = Modifier.Companion.size(24.dp))
        Spacer(Modifier.Companion.width(8.dp))
        DtHeader1("Compose Hot Reload Tooling")
        Spacer(Modifier.weight(1f))
        DtButton(
            onClick = onClose,
            modifier = Modifier
                .padding(2.dp)
                .size(24.dp)
        ) {
            Icon(
                Icons.Default.Close, contentDescription = "Close",
                modifier = Modifier.Companion.fillMaxSize()
            )
        }
    }
}
