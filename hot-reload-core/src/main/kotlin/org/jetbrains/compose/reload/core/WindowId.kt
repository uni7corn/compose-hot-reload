package org.jetbrains.compose.reload.core

import java.io.Serializable
import java.util.UUID

@JvmInline
public value class WindowId(public val value: String) : Serializable {
    public companion object {
        public fun create(): WindowId = WindowId(UUID.randomUUID().toString())
    }

    override fun toString(): String {
        return "WindowId(value='$value')"
    }
}
