/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.compose.reload.jvm.tooling.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object DtPadding {
    val horizontal = 8.dp
    val vertical = 8.dp

    val arrangement = 4.dp
}


fun Modifier.dtHorizontalPadding() = this.padding(horizontal = DtPadding.horizontal)

fun Modifier.dtVerticalPadding() = this.padding(vertical = DtPadding.vertical)
