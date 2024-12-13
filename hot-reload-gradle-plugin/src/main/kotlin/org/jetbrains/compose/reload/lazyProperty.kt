package org.jetbrains.compose.reload

import org.jetbrains.kotlin.tooling.core.HasMutableExtras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.getOrPut
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal fun <R : HasMutableExtras, T> lazyProperty(initializer: R.() -> T): ReadOnlyProperty<R, T> {
    return LazyProperty(initializer)
}

internal class LazyProperty<R : HasMutableExtras, T>(
    private val initializer: (R.() -> T)
) : ReadOnlyProperty<R, T> {

    override fun getValue(thisRef: R, property: KProperty<*>): T {
        val properties = thisRef.extras.getOrPut(propertiesKey) { mutableMapOf<LazyProperty<*, *>, Any?>() }
        @Suppress("UNCHECKED_CAST")
        return properties.getOrPut(this) {
            initializer.invoke(thisRef)
        } as T
    }

    private companion object {
        val propertiesKey = extrasKeyOf<MutableMap<LazyProperty<*, *>, Any?>>()
    }
}
