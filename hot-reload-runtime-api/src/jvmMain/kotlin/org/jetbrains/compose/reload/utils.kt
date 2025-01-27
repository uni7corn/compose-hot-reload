package org.jetbrains.compose.reload

import java.lang.ref.WeakReference

/**
 * Utility for tracking of a certain class was removed dynamically.
 */
internal fun interface ClassLifetimeToken {
    /**
     * @return 'true' when the class is still accessible (alive). 'false' if the class was removed.
     */
    fun isAlive(): Boolean
}

internal fun ClassLifetimeToken(instance: Any): ClassLifetimeToken {
    val classLoaderReference = WeakReference(instance::class.java.classLoader)
    val className = instance::class.java.name

    return ClassLifetimeToken {
        val classLoader = classLoaderReference.get() ?: return@ClassLifetimeToken false
        runCatching { classLoader.loadClass(className) }.getOrNull() != null
    }
}
