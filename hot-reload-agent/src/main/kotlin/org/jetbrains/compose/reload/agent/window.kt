/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.compose.reload.agent

import javassist.expr.ExprEditor
import javassist.expr.MethodCall
import org.jetbrains.compose.reload.analysis.ClassId
import org.jetbrains.compose.reload.analysis.Ids
import org.jetbrains.compose.reload.core.HotReloadEnvironment
import org.jetbrains.compose.reload.core.createLogger
import org.jetbrains.compose.reload.core.error
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

private val logger = createLogger()

/**
 * Transform Compose Desktop's 'Window' function to automatically setup the 'DevelopmentEntryPoint'
 */
internal fun launchWindowInstrumentation(instrumentation: Instrumentation) {
    if (!HotReloadEnvironment.devToolsEnabled) return
    if (HotReloadEnvironment.isHeadless) return
    instrumentation.addTransformer(WindowInstrumentation)
}

internal object WindowInstrumentation : ClassFileTransformer {
    override fun transform(
        loader: ClassLoader?, className: String?,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?, classfileBuffer: ByteArray?
    ): ByteArray? {
        if (classfileBuffer == null) return null
        if (className == null) return null
        if (// before CMP 1.9.0
            className.startsWith(Ids.WindowDesktopKt.classId.value) ||
            className.startsWith(Ids.DialogDesktopKt.classId.value) ||
            // CMP 1.9.0+
            className.startsWith(Ids.SwingWindowDesktopKt.classId.value) ||
            className.startsWith(Ids.SwingDialogDesktopKt.classId.value)
        ) {
            return transformSetContent(loader, classfileBuffer)
        }

        return null
    }
}

private fun transformSetContent(
    loader: ClassLoader?, classfileBuffer: ByteArray
): ByteArray? {
    val ctClass = getClassPool(loader ?: ClassLoader.getSystemClassLoader()).makeClass(classfileBuffer.inputStream())
    var transformed = false

    try {
        ctClass.instrument(object : ExprEditor() {
            override fun edit(m: MethodCall) {
                try {
                    when (m.methodId) {
                        Ids.ComposeWindow.setContent_1 ->  {
                            m.replace(wrapSetContent1(Ids.ComposeWindow.classId))
                            transformed = true
                        }
                        Ids.ComposeWindow.setContent_3 -> {
                            m.replace(wrapSetContent3(Ids.ComposeWindow.classId))
                            transformed = true
                        }
                        Ids.ComposeDialog.setContent_1 ->  {
                            m.replace(wrapSetContent1(Ids.ComposeDialog.classId))
                            transformed = true
                        }
                        Ids.ComposeDialog.setContent_3 -> {
                            m.replace(wrapSetContent3(Ids.ComposeDialog.classId))
                            transformed = true
                        }
                    }
                } catch (t: Throwable) {
                    logger.error("Failed to transform 'setContent' method", t)
                }
            }
        })

        return if (transformed) ctClass.toBytecode() else null
    } catch (t: Throwable) {
        logger.error("Failed to transform 'WindowKt'", t)
        return null
    }
}

private fun wrapSetContent1(windowClass: ClassId): String = """
    {
        // Store parameters in local variables to maintain stack
        ${windowClass.toFqn()} window = $0;
        kotlin.jvm.functions.Function3 p1= $1;
        
        // Call the development entry point with stored parameters
        org.jetbrains.compose.reload.jvm.JvmDevelopmentEntryPoint.setContent(
            window, p1
        );
    }
    """.trimIndent()

private fun wrapSetContent3(windowClass: ClassId): String = """
    {
        // Store parameters in local variables to maintain stack
        ${windowClass.toFqn()} window = $0;
        kotlin.jvm.functions.Function1 p1 = $1;
        kotlin.jvm.functions.Function1 p2 = $2;
        kotlin.jvm.functions.Function3 p3 = $3;
        
        // Call the development entry point with stored parameters
        org.jetbrains.compose.reload.jvm.JvmDevelopmentEntryPoint.setContent(
            window, p1, p2, p3
        );
    }
    """.trimIndent()
