@file:JvmName("DevApplicationHeadless")

package org.jetbrains.compose.reload.jvm

import androidx.compose.runtime.Composable
import kotlin.time.Duration

@Suppress("unused")
public fun runDevApplicationHeadless(
    timeout: Duration,
    width: Int, height: Int,
    content: @Composable () -> Unit
) {
    error("'runDevApplicationHeadless' requires the 'dev' variant of hot-reload-runtime")
}