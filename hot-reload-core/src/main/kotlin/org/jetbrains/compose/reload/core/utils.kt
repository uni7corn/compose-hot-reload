package org.jetbrains.compose.reload.core

public fun <T> Iterator<T>.nextOrNull(): T? = if (hasNext()) next() else null
