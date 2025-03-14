/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

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
