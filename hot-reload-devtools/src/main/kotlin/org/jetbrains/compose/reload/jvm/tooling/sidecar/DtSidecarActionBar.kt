/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.compose.reload.jvm.tooling.sidecar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.reload.core.HotReloadEnvironment
import org.jetbrains.compose.reload.jvm.tooling.send
import org.jetbrains.compose.reload.jvm.tooling.widgets.DtTextButton
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DtSidecarActionBar(modifier: Modifier = Modifier.Companion) {
    FlowRow(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        if (!HotReloadEnvironment.gradleBuildContinuous) {
            DtTextButton("Reload", onClick = {
                OrchestrationMessage.RecompileRequest().send()
            })
        }

        DtTextButton("Exit", onClick = {
            OrchestrationMessage.ShutdownRequest().send()
        })

        DtTextButton("Retry Failed Compositions", onClick = {
            OrchestrationMessage.RetryFailedCompositionRequest().send()
        })


        DtTextButton("Clean Composition", onClick = {
            OrchestrationMessage.CleanCompositionRequest().send()
        })

    }
}
