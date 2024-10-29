@file:JvmName("DevApplication")

package org.jetbrains.compose.reload.jvm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType.methodType
import kotlin.time.Duration.Companion.minutes

@OptIn(InternalHotReloadApi::class)
internal fun main(args: Array<String>) {
    /* Parse arguments */
    var className: String? = null
    var funName: String? = null

    val argsIterator = args.toList().listIterator()
    while (argsIterator.hasNext()) {
        when (val value = argsIterator.next()) {
            "--className" -> className = argsIterator.next()
            "--funName" -> funName = argsIterator.next()
            else -> error("Unknown argument: $value")
        }
    }

    className ?: error("Missing --className argument")
    funName ?: error("Missing --funName argument")

    /* Find method and meta information */
    val resolvedClass = Class.forName(className)
    val method = resolvedClass.getDeclaredMethod(funName, Composer::class.java, Int::class.javaPrimitiveType)
    val annotation = method.getDeclaredAnnotation(DevelopmentEntryPoint::class.java)

    if (System.getProperty("compose.reload.headless") == "true") {
        startHeadlessApplication(
            width = annotation.windowWidth,
            height = annotation.windowWidth,
            timeout = 5.minutes
        ) {
            JvmDevelopmentEntryPoint {
                invokeUI(resolvedClass, funName)
            }
        }
        return
    }

    singleWindowApplication(
        title = "Dev Run",
        alwaysOnTop = true,
        state = WindowState(size = DpSize(annotation.windowWidth.dp, annotation.windowHeight.dp)),
    ) {
        JvmDevelopmentEntryPoint {
            invokeUI(resolvedClass, funName)
        }
    }
}


@Composable
private fun invokeUI(uiClass: Class<*>, funName: String) {
    val uiMethodHandle = MethodHandles.lookup().findStatic(
        uiClass, funName,
        methodType(Void.TYPE, Composer::class.java, Int::class.javaPrimitiveType)
    )


    invokeUI(uiMethodHandle)
}

@Composable
private fun invokeUI(ui: MethodHandle) {
    currentComposer.startRestartGroup(1902)
    ui.invokeWithArguments(currentComposer, 0 /* 0 means not changed!*/)

    currentComposer.endRestartGroup()?.updateScope { composer, i ->
        ui.invokeWithArguments(composer, i)
    }
}
