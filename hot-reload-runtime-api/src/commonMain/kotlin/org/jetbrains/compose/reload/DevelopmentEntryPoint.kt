package org.jetbrains.compose.reload

import androidx.compose.runtime.Composable

@Composable
public expect fun DevelopmentEntryPoint(child: @Composable () -> Unit)

public annotation class DevelopmentEntryPoint(
    val windowHeight: Int = 1024,
    val windowWidth: Int = 576,
)