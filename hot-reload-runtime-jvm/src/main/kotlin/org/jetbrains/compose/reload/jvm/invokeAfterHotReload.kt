@file:JvmName("JvmInvokeAfterHotReload")

package org.jetbrains.compose.reload.jvm

private val noopAutoCloseable = AutoCloseable { }

public fun invokeAfterHotReload(@Suppress("unused") block: () -> Unit): AutoCloseable {
    return noopAutoCloseable
}
