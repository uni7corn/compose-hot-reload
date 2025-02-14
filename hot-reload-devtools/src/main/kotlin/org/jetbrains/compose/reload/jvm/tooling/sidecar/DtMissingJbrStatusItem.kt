/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.compose.reload.jvm.tooling.sidecar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import org.jetbrains.compose.reload.jvm.tooling.theme.DtColors
import org.jetbrains.compose.reload.jvm.tooling.widgets.DtText

@Composable
fun DtMissingJbrStatusItem() {
    val vendor = System.getProperty("java.vendor")
    if (vendor.contains("JetBrains", ignoreCase = true)) return

    DtSidecarStatusItem(
        symbol = { Icon(Icons.Default.Warning, "Missing JBR", tint = DtColors.statusColorWarning) },
        content = { DtText("Not running on 'JetBrains Runtime' (Current jvm: $vendor)") }
    )
}
