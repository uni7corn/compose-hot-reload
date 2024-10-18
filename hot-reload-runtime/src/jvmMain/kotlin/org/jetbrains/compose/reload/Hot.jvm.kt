package org.jetbrains.compose.reload

import androidx.compose.runtime.Composable

@Composable
public actual fun Hot(child: @Composable () -> Unit) {
    HotReload { child() }
}