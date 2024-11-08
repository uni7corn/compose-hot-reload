package org.jetbrains.compose.reload.agent

import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.LogMessage.Companion.TAG_AGENT
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.LogMessage.Companion.TAG_RUNTIME
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles


@Suppress("NOTHING_TO_INLINE") // We want the caller class!
@JvmName("createLookupLogger")
internal inline fun createLogger() = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

internal inline fun <reified T : Any> createLogger() = LoggerFactory.getLogger(T::class.java)

@Suppress("NOTHING_TO_INLINE") // We want the caller class!
public inline fun Logger.orchestration(message: String, throwable: Throwable? = null) {
    if (throwable != null) {
        error(message, throwable)
    } else {
        info(message)
    }

    val packageName = MethodHandles.lookup().lookupClass().packageName
    val tag = when {
        packageName.startsWith(ComposeHotReloadAgent.javaClass.packageName) -> TAG_AGENT
        else -> TAG_RUNTIME
    }

    val message = if (throwable == null) message else buildString {
        appendLine(message)
        append(throwable.stackTraceToString())
    }

    OrchestrationMessage.LogMessage(tag, message).send()
}
