/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.compose.reload.core

public sealed interface Context {
    public operator fun <T> get(key: ContextKey<T>): T
}

public interface ContextKey<out T> {
    public val default: T
}

public abstract class OptionalContextKey<T> : ContextKey<T?> where T : Any {
    final override val default: T? get() = null
}

public abstract class ContextElement : Context {
    public abstract val key: ContextKey<*>

    override fun <T> get(key: ContextKey<T>): T {
        @Suppress("UNCHECKED_CAST")
        return if (key == this.key) this as T
        else key.default
    }
}

public fun Context(): Context {
    return EmptyContext
}

public fun Context(vararg entries: ContextEntry<*>): Context {
    if (entries.isEmpty()) return Context()
    else return ContextImpl(entries.associate { it.key to it.value })
}

public fun Context.with(vararg entries: ContextEntry<*>): Context {
    if (entries.isEmpty()) return this
    return ContextImpl(this.map + entries.associate { it.key to it.value })
}

public operator fun Context.plus(other: Context): Context {
    val thisMap = this.map
    val otherMap = other.map
    if (thisMap.isEmpty()) return other
    if (otherMap.isEmpty()) return this
    return ContextImpl(thisMap + otherMap)
}

public infix fun <T> ContextKey<T>.with(value: T): ContextEntry<T> =
    ContextEntry(this, value)

public class ContextEntry<T>(
    public val key: ContextKey<T>,
    public val value: T,
)

public object EmptyContext : Context {
    override fun <T> get(key: ContextKey<T>): T {
        return key.default
    }
}

private class ContextImpl(
    val map: Map<ContextKey<*>, Any?>
) : Context {
    override fun <T> get(key: ContextKey<T>): T {
        @Suppress("UNCHECKED_CAST")
        return if (key in map) map[key] as T
        else key.default
    }
}

private val Context.map: Map<ContextKey<*>, Any?>
    get() = when (this) {
        is ContextImpl -> this.map
        is ContextElement -> mapOf(key to this)
        is EmptyContext -> emptyMap()
    }
