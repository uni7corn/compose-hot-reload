@file:JvmName("JvmDevelopmentEntryPoint")

package org.jetbrains.compose.reload.jvm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.currentRecomposeScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.reload.InternalHotReloadApi
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent
import org.jetbrains.compose.reload.agent.send
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage

private val logger = createLogger()

@Composable
@PublishedApi
@InternalHotReloadApi
internal fun JvmDevelopmentEntryPoint(child: @Composable () -> Unit) {
    HotReloadComposable {
        runCatching { child() }.onFailure { exception ->
            logger.error("Failed invoking 'JvmDevelopmentEntryPoint':", exception)
            currentRecomposeScope.invalidate()

            OrchestrationMessage.UIException(
                message = exception.message,
                stacktrace = exception.stackTrace.toList()
            ).send()

        }.getOrThrow()
    }
}
