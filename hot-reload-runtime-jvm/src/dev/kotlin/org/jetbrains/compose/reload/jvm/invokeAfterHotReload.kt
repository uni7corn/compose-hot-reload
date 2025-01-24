@file:JvmName("JvmInvokeAfterHotReload")
@file:Suppress("unused")

package org.jetbrains.compose.reload.jvm

import org.jetbrains.compose.reload.agent.invokeAfterHotReload
import org.jetbrains.compose.reload.core.isSuccess
import javax.swing.SwingUtilities

/**
 * Special _hidden_ system property which can be used to debug exceptions thrown in 'invokeAfterHotReload'.
 */
private val enableInvokeAfterHotReloadStackTrace =
    System.getProperty("compose.reload.invokeAfterHotReloadStackTraceEnabled") == "true"

private class InvokeAfterHotReloadException(message: String, override val cause: Throwable?) : Exception(message, cause)

private class InvokeAfterHotReloadRegistration : Exception("called 'invokeAfterHotReload'")

fun invokeAfterHotReload(@Suppress("unused") block: () -> Unit): AutoCloseable {
    val registrationTrace = if (enableInvokeAfterHotReloadStackTrace) Thread.currentThread().stackTrace else null

    val disposable = invokeAfterHotReload { _, result ->
        if (result.isSuccess()) {
            try {
                block()
            } catch (_: NoSuchMethodError) {
                // https://github.com/JetBrains/compose-hot-reload/issues/66
            } catch (t: Throwable) {
                val exception = InvokeAfterHotReloadException("Exception in 'invokeAfterHotReload' block", t)
                if (registrationTrace != null) {
                    exception.stackTrace = registrationTrace
                }

                SwingUtilities.invokeLater {
                    throw exception
                }
            }
        }
    }

    return AutoCloseable {
        disposable.dispose()
    }
}
