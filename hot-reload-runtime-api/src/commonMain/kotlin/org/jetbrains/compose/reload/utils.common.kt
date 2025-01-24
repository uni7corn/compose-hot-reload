package org.jetbrains.compose.reload

internal val noopAutoCloseable = AutoCloseable {}

internal object NoopHotReloadScope : HotReloadScope() {
    override fun invokeAfterHotReload(action: () -> Unit): AutoCloseable {
        return noopAutoCloseable
    }
}
