package org.jetbrains.compose.reload.jvm.tooling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal val devToolingScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
