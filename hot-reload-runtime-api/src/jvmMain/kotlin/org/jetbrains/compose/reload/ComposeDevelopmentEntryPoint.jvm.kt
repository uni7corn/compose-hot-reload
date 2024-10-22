package org.jetbrains.compose.reload

import androidx.compose.runtime.Composable
import org.jetbrains.compose.reload.jvm.ComposeDevelopmentEntryPointJvm
import org.jetbrains.compose.reload.jvm.InternalHotReloadApi

@Composable
public actual fun ComposeDevelopmentEntryPoint(child: @Composable () -> Unit) {
    @OptIn(InternalHotReloadApi::class)
    ComposeDevelopmentEntryPointJvm(child)
}