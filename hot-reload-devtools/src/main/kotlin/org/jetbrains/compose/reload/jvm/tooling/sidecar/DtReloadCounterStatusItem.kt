/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.compose.reload.jvm.tooling.sidecar

import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.sellmair.evas.compose.composeValue
import org.jetbrains.compose.reload.jvm.tooling.Tag
import org.jetbrains.compose.reload.jvm.tooling.Tag.ReloadCounterText
import org.jetbrains.compose.reload.jvm.tooling.states.ReloadCountState
import org.jetbrains.compose.reload.jvm.tooling.tag
import org.jetbrains.compose.reload.jvm.tooling.widgets.DtText

@Composable
fun DtReloadCounterStatusItem() {
    val state = ReloadCountState.composeValue()

    if (state.successfulReloads > 0) {
        DtSidecarStatusItem(
            symbol = {
                Icon(Icons.Default.Refresh, "Reload")
            },
            content = {
                DtText("${state.successfulReloads} successful reloads", modifier = Modifier.tag(ReloadCounterText))
            }
        )
    }
}
