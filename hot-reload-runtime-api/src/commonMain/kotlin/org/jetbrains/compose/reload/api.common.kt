package org.jetbrains.compose.reload

import androidx.compose.runtime.Composable

@RequiresOptIn("Internal API: Do not use!", RequiresOptIn.Level.ERROR)
public annotation class InternalHotReloadApi

@Composable
public expect fun DevelopmentEntryPoint(child: @Composable () -> Unit)

public annotation class DevelopmentEntryPoint(
    val windowWidth: Int = 576,
    val windowHeight: Int = 1024,
)
