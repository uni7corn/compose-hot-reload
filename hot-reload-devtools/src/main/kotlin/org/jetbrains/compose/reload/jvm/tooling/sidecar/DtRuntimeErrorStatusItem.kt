/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.compose.reload.jvm.tooling.sidecar

import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import io.sellmair.evas.compose.composeValue
import org.jetbrains.compose.reload.jvm.tooling.states.UIErrorState
import org.jetbrains.compose.reload.jvm.tooling.theme.DtColors
import org.jetbrains.compose.reload.jvm.tooling.widgets.DtText

@Composable
fun DtRuntimeErrorStatusItem() {
    val error = UIErrorState.composeValue()

    error.errors.forEach { error ->
        DtSidecarStatusItem(
            symbol = { Icon(Icons.Default.Warning, "Error", tint = DtColors.statusColorError) },
            content = {
                DtText("Exception: ${error.value.title} (${error.value.message})")
            }
        )
    }
}
