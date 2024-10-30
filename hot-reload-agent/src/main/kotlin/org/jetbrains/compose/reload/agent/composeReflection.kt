package org.jetbrains.compose.reload.agent

private const val RECOMPOSER_CLASS = "androidx.compose.runtime.Recomposer"
private const val COMPANION_FIELD = "Companion"
private const val RECOMPOSER_COMPANION_CLASS = "$RECOMPOSER_CLASS\$$COMPANION_FIELD"

internal fun enableComposeHotReloadMode(moduleClassLoader: ClassLoader = Thread.currentThread().contextClassLoader) {
    try {
        val recomposerClass = moduleClassLoader.loadClass(RECOMPOSER_CLASS)
        val recomposerCompanion = recomposerClass.getField(COMPANION_FIELD).get(null)
        val recomposerCompanionClass = moduleClassLoader.loadClass(RECOMPOSER_COMPANION_CLASS)
        recomposerCompanionClass.methods
            .singleOrNull { it.name.contains("setHotReloadEnabled") }
            ?.apply { invoke(recomposerCompanion, true) }

        logger.debug("'setHotReloadEnabled' method found, enabled compose hot reload mode")
    } catch (e: ReflectiveOperationException) {
        logger.warn("Failed to enable compose hot reload mode", e)
    }
}