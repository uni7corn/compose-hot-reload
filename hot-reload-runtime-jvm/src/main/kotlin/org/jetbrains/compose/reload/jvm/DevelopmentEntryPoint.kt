@file:JvmName("JvmDevelopmentEntryPoint")

package org.jetbrains.compose.reload.jvm

import androidx.compose.runtime.Composable

@Composable
public fun DevelopmentEntryPoint(child: @Composable () -> Unit) {
    child()
}
