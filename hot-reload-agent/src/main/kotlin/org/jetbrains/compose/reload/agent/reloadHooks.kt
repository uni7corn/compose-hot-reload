package org.jetbrains.compose.reload.agent

import org.jetbrains.compose.reload.core.Disposable
import org.jetbrains.compose.reload.core.Try
import java.util.UUID

private val beforeReloadListeners =
    mutableListOf<(reloadRequestId: UUID) -> Unit>()

private val afterReloadListeners =
    mutableListOf<(reloadRequestId: UUID, result: Try<Reload>) -> Unit>()

fun invokeBeforeHotReload(block: (reloadRequestId: UUID) -> Unit): Disposable =
    synchronized(beforeReloadListeners) {
        beforeReloadListeners.add(block)
        Disposable {
            synchronized(beforeReloadListeners) {
                beforeReloadListeners.remove(block)
            }
        }
    }

fun invokeAfterHotReload(block: (reloadRequestId: UUID, result: Try<Reload>) -> Unit): Disposable =
    synchronized(afterReloadListeners) {
        afterReloadListeners.add(block)
        Disposable {
            synchronized(afterReloadListeners) {
                afterReloadListeners.remove(block)
            }
        }
    }

internal fun executeBeforeHotReloadListeners(reloadRequestId: UUID) {
    val listeners = synchronized(beforeReloadListeners) { beforeReloadListeners.toList() }
    listeners.forEach { listener -> listener.invoke(reloadRequestId) }
}

internal fun executeAfterHotReloadListeners(reloadRequestId: UUID, result: Try<Reload>) {
    val listeners = synchronized(afterReloadListeners) { afterReloadListeners.toList() }
    listeners.forEach { listener -> listener.invoke(reloadRequestId, result) }
}
