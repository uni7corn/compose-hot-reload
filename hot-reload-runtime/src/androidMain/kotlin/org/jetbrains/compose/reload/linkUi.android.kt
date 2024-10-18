package org.jetbrains.compose.reload

import android.annotation.TargetApi
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.currentComposer
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType.methodType

@TargetApi(Build.VERSION_CODES.O)
@Composable
public actual fun linkUI(className: String, funName: String) {
    val uiClass = Class.forName(className)

    val uiMethodHandle = MethodHandles.lookup().findStatic(
        uiClass, funName, methodType(Void.TYPE, Composer::class.java, Int::class.javaPrimitiveType)
    )

    invokeUI(uiMethodHandle)
}

@TargetApi(Build.VERSION_CODES.O)
@Composable
private fun invokeUI(ui: MethodHandle) {
    ui.invokeWithArguments(currentComposer, 0 /* 0 means not changed!*/)
}
