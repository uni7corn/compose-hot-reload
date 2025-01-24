@file:JvmName("HotReloadApi")

package org.jetbrains.compose.reload

import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import org.jetbrains.compose.reload.jvm.JvmDevelopmentEntryPoint

@OptIn(InternalComposeApi::class)
@Composable
public actual fun DevelopmentEntryPoint(child: @Composable () -> Unit) {
    JvmDevelopmentEntryPoint(child)
}
