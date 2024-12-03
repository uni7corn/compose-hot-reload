package org.jetbrains.compose.reload.core

import java.util.concurrent.atomic.AtomicReference

public data class Update<T>(val previous: T, val updated: T)

public inline fun <T> AtomicReference<T>.update(updater: (T) -> T): Update<T> {
    while (true) {
        val value = get()
        val updated = updater(value)
        if (compareAndSet(value, updated)) {
            return Update(value, updated)
        }
    }
}