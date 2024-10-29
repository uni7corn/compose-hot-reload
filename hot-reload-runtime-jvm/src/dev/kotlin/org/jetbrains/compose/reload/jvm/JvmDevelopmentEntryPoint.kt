@file:JvmName("JvmDevelopmentEntryPoint")

package org.jetbrains.compose.reload.jvm

import androidx.compose.runtime.Composable
import org.jetbrains.compose.reload.InternalHotReloadApi

@Composable
@PublishedApi
@InternalHotReloadApi
internal fun JvmDevelopmentEntryPoint(child: @Composable () -> Unit) {
    HotReloadComposable { child() }
}
