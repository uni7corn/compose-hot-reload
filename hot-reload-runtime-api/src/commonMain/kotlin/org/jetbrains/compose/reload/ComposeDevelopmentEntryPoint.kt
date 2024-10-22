package org.jetbrains.compose.reload

import androidx.compose.runtime.Composable

@Composable
@Deprecated("Use 'ComposeDevelopmentEntryPoint' instead", ReplaceWith("ComposeDevelopmentEntryPoint(child)"))
public fun Hot(child: @Composable () -> Unit): Unit = ComposeDevelopmentEntryPoint(child)

@Composable
@Deprecated("Use 'ComposeDevelopmentEntryPoint' instead", ReplaceWith("ComposeDevelopmentEntryPoint(child)"))
public fun HotReload(child: @Composable () -> Unit): Unit = ComposeDevelopmentEntryPoint(child)

@Composable
public expect fun ComposeDevelopmentEntryPoint(child: @Composable () -> Unit)