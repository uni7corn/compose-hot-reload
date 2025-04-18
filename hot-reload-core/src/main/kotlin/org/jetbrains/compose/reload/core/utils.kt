/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.compose.reload.core

public fun <T> Iterator<T>.nextOrNull(): T? = if (hasNext()) next() else null

public inline fun <reified T> name(): String {
    return T::class.java.name
}

public inline fun <reified T> simpleName(): String {
    return T::class.java.simpleName
}

public fun hashCode(vararg values: Any?): Int = values.contentHashCode()
