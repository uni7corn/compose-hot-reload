package org.jetbrains.compose.reload

import androidx.compose.runtime.Composable

@Composable
public expect fun ComposeDevelopmentEntryPoint(child: @Composable () -> Unit)