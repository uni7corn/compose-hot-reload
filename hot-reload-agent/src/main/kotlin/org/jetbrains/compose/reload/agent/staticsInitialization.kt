package org.jetbrains.compose.reload.agent

import javassist.CtClass
import javassist.CtConstructor
import org.jetbrains.compose.reload.core.createLogger
import java.lang.instrument.ClassDefinition
import java.lang.reflect.Modifier

private val logger = createLogger()

internal const val reinitializeName = "\$chr\$clinit"

/**
 * Re-initialize changed static
 * 1) We demote 'static final' to 'non-final'
 * 2) We create a static function which contains the class initializer body
 * 3) We call the new static function to re-initialize the class.
 */
internal fun CtClass.transformForStaticsInitialization(originalClass: Class<*>?) {
    if (originalClass == null) return

    val originalFields = originalClass.declaredFields.associate { field ->
        field.name to field.type.name
    }

    val newFields = declaredFields.associate { field ->
        field.name to field.type.name
    }

    val clazzInitializer = classInitializer
    if (clazzInitializer != null && originalFields != newFields) {
        declaredFields.forEach { field ->
            field.modifiers = field.modifiers and Modifier.FINAL.inv()
        }

        logger.debug("Created synthetic re-initializer for '${name}")
        val reinit = CtConstructor(clazzInitializer, this, null)
        reinit.methodInfo.name = reinitializeName
        reinit.modifiers = Modifier.PUBLIC or Modifier.STATIC
        addConstructor(reinit)
    }
}

internal fun ClassDefinition.reinitializeStaticsIfNecessary() {
    val reinit = runCatching { definitionClass.getDeclaredMethod(reinitializeName) }.getOrNull() ?: return
    logger.debug("Re-initializing ${definitionClass.name}")
    reinit.trySetAccessible()
    reinit.invoke(null)
    logger.debug("Re-initialized ${definitionClass.name}")
}
