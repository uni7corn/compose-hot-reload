/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.compose.reload.jvm.tooling.theme

import androidx.compose.ui.graphics.Color

object DtColors {
    val composeLogo = Color(red = 66, green = 133, blue = 244)
    val applicationBackground = Color.White.copy(alpha = 0.9f)

    val surface = Color(red = 244, green = 248, blue = 255)
    val surfaceActive = Color(red = 198, green = 221, blue = 255)

    val statusColorOk = Color(red = 56, green = 207, blue = 96)
    val statusColorOrange1 = Color(0xfff4d642)
    val statusColorOrange2 = Color(0xffff8c45)
    val statusColorError = Color(0xffe74c3c)

    val text = Color(red = 51, green = 51, blue = 51)
}
