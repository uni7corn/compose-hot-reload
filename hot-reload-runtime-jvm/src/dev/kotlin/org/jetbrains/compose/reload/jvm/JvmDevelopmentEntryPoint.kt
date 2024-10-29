package org.jetbrains.compose.reload.jvm

import androidx.compose.runtime.Composable

@Composable
@InternalHotReloadApi
fun JvmDevelopmentEntryPoint(child: @Composable () -> Unit) {
    HotReload { child() }
}
