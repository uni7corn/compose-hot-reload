@file:JvmName("HotReloadApi")

package org.jetbrains.compose.reload

import androidx.compose.runtime.Composable
import kotlin.jvm.JvmName

@Composable
public actual fun DevelopmentEntryPoint(child: @Composable () -> Unit) {
    child()
}

public actual val staticHotReloadScope: HotReloadScope = NoopHotReloadScope

@Composable
public actual fun AfterHotReloadEffect(action: () -> Unit): Unit = Unit
