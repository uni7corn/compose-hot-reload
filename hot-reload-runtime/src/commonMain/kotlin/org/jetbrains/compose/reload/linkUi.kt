package org.jetbrains.compose.reload

import androidx.compose.runtime.Composable

@Composable
public expect fun linkUI(className: String, funName: String = "Main")