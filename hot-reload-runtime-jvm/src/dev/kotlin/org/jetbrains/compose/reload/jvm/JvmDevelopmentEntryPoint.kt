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
    launchErrorRecovery()

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

/**
 * As long as this composable is in the tree, successful reloads will try to call into the
 * Compose Recovery machinery.
 * Note: A public API would be great here.
 */
@NonRestartableComposable
@Composable
private fun launchErrorRecovery() {
    LaunchedEffect(Unit) {
        val registration = ComposeHotReloadAgent.invokeAfterReload { _, exception ->
            runBlocking(Dispatchers.Main) {
                if (exception == null) {
                    logger.info("Recomposer: loadStateAndComposeForHotReload")
                    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
                    androidx.compose.runtime.Recomposer.loadStateAndComposeForHotReload(emptyList<Any>())
                }
            }
        }

        currentCoroutineContext().job.invokeOnCompletion {
            logger.debug("ErrorRecovery: Goodbye")
            registration.dispose()
        }
        awaitCancellation()
    }
}
