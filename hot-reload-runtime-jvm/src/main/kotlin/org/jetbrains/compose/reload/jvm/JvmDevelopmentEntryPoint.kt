@file:JvmName("JvmDevelopmentEntryPoint")

package org.jetbrains.compose.reload.jvm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi

@Composable
@InternalComposeApi
public fun JvmDevelopmentEntryPoint(child: @Composable () -> Unit) {
    child()
}
