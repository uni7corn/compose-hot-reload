package org.jetbrains.compose.reload.agent

private const val RECOMPOSER_CLASS = "androidx.compose.runtime.Recomposer"
private const val COMPANION_FIELD = "Companion"
private const val RECOMPOSER_COMPANION_CLASS = "$RECOMPOSER_CLASS\$$COMPANION_FIELD"


internal fun resetComposeErrors(classLoader: ClassLoader = Thread.currentThread().contextClassLoader) {
    try {
        val recomposer = classLoader.loadClass(RECOMPOSER_CLASS)
        val recomposerCompanion = recomposer.getField(COMPANION_FIELD).get(null)
        val recomposerCompanionClass = classLoader.loadClass(RECOMPOSER_COMPANION_CLASS)

        val clearErrorsMethod = recomposerCompanionClass.methods
            .filter { it.name.contains("clearErrors") }

        if (clearErrorsMethod.isEmpty()) {
            logger.warn("Missing 'clearErrors' method")
            return
        }

        if (clearErrorsMethod.size > 1) {
            logger.warn("Ambiguous 'clearErrors' methods: ${clearErrorsMethod.map { it.name }}")
        }

        clearErrorsMethod.first().invoke(recomposerCompanion)
        logger.debug("Compose errors were reset")
    } catch (t: Throwable) {
        logger.warn("Failed to reset compose errors", t)
    }
}

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