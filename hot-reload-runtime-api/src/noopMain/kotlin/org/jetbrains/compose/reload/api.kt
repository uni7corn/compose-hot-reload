@file:JvmName("HotReloadApi")

package org.jetbrains.compose.reload

import androidx.compose.runtime.Composable
import kotlin.jvm.JvmName

@Composable
public actual fun DevelopmentEntryPoint(child: @Composable () -> Unit) {
    child()
}
