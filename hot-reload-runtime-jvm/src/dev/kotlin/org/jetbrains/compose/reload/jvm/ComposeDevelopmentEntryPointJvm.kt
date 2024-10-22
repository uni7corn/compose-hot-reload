package org.jetbrains.compose.reload.jvm

import androidx.compose.runtime.Composable

@Composable
@InternalHotReloadApi
public fun ComposeDevelopmentEntryPointJvm(child: @Composable () -> Unit) {
    HotReload { child() }
}

@RequiresOptIn("Internal API: Do not use!", RequiresOptIn.Level.ERROR)
annotation class InternalHotReloadApi