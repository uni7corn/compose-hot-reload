package org.jetbrains.compose.reload.agent

import javassist.ClassPool
import javassist.CtConstructor
import javassist.LoaderClassPath
import javassist.expr.ExprEditor
import javassist.expr.MethodCall
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.lang.instrument.ClassDefinition
import java.lang.instrument.Instrumentation
import java.lang.reflect.Modifier
import kotlin.concurrent.withLock

private const val reinitializeName = "\$cr\$clinit"

private val pool = ClassPool().apply {
    appendClassPath(LoaderClassPath(ClassLoader.getSystemClassLoader()))
}

internal fun reload(
    instrumentation: Instrumentation, pendingChanges: Map<File, OrchestrationMessage.ReloadClassesRequest.ChangeType>
) = ComposeHotReloadAgent.reloadLock.withLock {
    val definitions = pendingChanges.mapNotNull { (file, change) ->
        if (change == OrchestrationMessage.ReloadClassesRequest.ChangeType.Removed) {
            return@mapNotNull null
        }

        if (file.extension != "class") {
            logger.warn("$change: $file is not a class")
            return@mapNotNull null
        }

        if (!file.isFile) {
            logger.warn("$change: $file is not a regular file")
            return@mapNotNull null
        }

        logger.debug("Loading: $file")

        val code = file.readBytes()
        val clazz = pool.makeClass(code.inputStream())

        val originalClass = runCatching {
            Class.forName(clazz.name)
        }.getOrNull()

        logger.debug(buildString {
            appendLine("Reloading class:'${clazz.name}'")

            if (originalClass?.superclass?.name != clazz.superclass.name) {
                appendLine("⚠️ Superclass: '${originalClass?.superclass?.name}' -> '${clazz.superclass?.name}'")
            }

            val addedInterfaces = clazz.interfaces.map { it.name } -
                    originalClass?.interfaces?.map { it.name }.orEmpty()
            addedInterfaces.forEach { addedInterface ->
                appendLine("⚠️ +Interface: '$addedInterface'")
            }

            val removedInterfaces = originalClass?.interfaces.orEmpty().map { it.name }.toSet() -
                    clazz.interfaces.map { it.name }.toSet()
            removedInterfaces.forEach { removedInterface ->
                appendLine("⚠️ -Interface: '$removedInterface'")
            }
        }.trim())


        /**
         * Instrumenting calls to 'rememberedValue':
         * If the remembered object was a function/lambda, then we "reject" the value by returning
         * the 'Composer.Empty' object which will result in the re-creation and update of the remembered value.
         */
        clazz.declaredMethods.forEach { method ->
            if (method.hasAnnotation("androidx.compose.runtime.Composable")) {
                method.instrument(object : ExprEditor() {
                    override fun edit(methodCall: MethodCall) {
                        val d = "\$"
                        if (methodCall.method.longName == "androidx.compose.runtime.Composer.rememberedValue()") {
                            logger.debug("Instrumenting '${method.longName} (${methodCall.lineNumber})'")

                            methodCall.replace(
                                """
                                {
                                    Object result = ${d}proceed($$);                                    
                                    if(result instanceof androidx.compose.runtime.internal.ComposableLambda ||
                                        result instanceof kotlin.Function
                                    ) {
                                        ${d}_ = androidx.compose.runtime.Composer.Companion.getEmpty();
                                    } else {
                                        ${d}_ = result;
                                    }
                                }
                            """.trimIndent()
                            )
                        }
                    }
                })
            }
        }

        /**
         * Re-initialize changed static
         * 1) We demote 'static final' to 'non-final'
         * 2) We create a static function which contains the class initializer body
         * 3) We call the new static function to re-initialize the class.
         */
        val clazzInitializer = clazz.classInitializer
        if (clazzInitializer != null && originalClass != null) {
            clazz.fields.forEach { field ->
                field.modifiers = field.modifiers and Modifier.FINAL.inv()
            }

            logger.debug("Created synthetic re-initializer for '${clazz.name}")
            val reinit = CtConstructor(clazzInitializer, clazz, null)
            reinit.methodInfo.name = reinitializeName
            reinit.modifiers = Modifier.PUBLIC or Modifier.STATIC
            clazz.addConstructor(reinit)
        }

        val baos = ByteArrayOutputStream()
        val daos = DataOutputStream(baos)
        clazz.classFile.write(daos)

        ClassDefinition(Class.forName(clazz.name), baos.toByteArray())
    }

    instrumentation.redefineClasses(*definitions.toTypedArray())

    definitions.forEach { definition ->
        val clazz = Class.forName(definition.definitionClass.name)
        val reinit = runCatching { clazz.getDeclaredMethod(reinitializeName) }.getOrNull() ?: return@forEach
        logger.debug("Re-initializing ${clazz.name}")
        reinit.trySetAccessible()
        reinit.invoke(null)
        logger.debug("Re-initialized ${clazz.name}")
    }
}

