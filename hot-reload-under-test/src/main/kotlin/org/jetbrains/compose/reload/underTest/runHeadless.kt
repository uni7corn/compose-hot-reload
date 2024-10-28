@file:OptIn(ExperimentalTestApi::class)

package org.jetbrains.compose.reload.underTest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.runDesktopComposeUiTest
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent
import org.jetbrains.compose.reload.orchestration.OrchestrationClient
import org.jetbrains.compose.reload.orchestration.OrchestrationClientRole
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ShutdownRequest
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.TakeScreenshotRequest
import org.jetbrains.compose.reload.orchestration.asChannel
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes


internal val applicationScope = CoroutineScope(Dispatchers.Default.limitedParallelism(1) + SupervisorJob())
internal val orchestration = OrchestrationClient(OrchestrationClientRole.Unknown) ?: error("Failed connecting to orchestration")
internal val messages = orchestration.asChannel()

/**
 * Runs a headless application which supports
 * - listening to [OrchestrationMessage.TestEvent]
 * - responding to [OrchestrationMessage.TakeScreenshotRequest]
 */
internal fun runHeadless(timeout: Int = 5, content: @Composable UnderTestApplication.() -> Unit) {
    runDesktopComposeUiTest(width = 256, height = 256) {
        setContent {
            Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                DevelopmentEntryPoint {
                    UnderTestApplication.content()
                }
            }
        }

        launchHeadlessOrchestration(timeout.minutes, this)
            .asCompletableFuture()
            .join()
    }
}

private fun launchHeadlessOrchestration(timeout: Duration, test: SkikoComposeUiTest): Job {
    /*
    Launch a coroutine which will kill the application after the timeout has passed.
    The application is supposed to finish before the timeout.
     */
    applicationScope.launch {
        delay(timeout)
        logger.error("Application timed out...")
        exitProcess(-1)
    }

    /*
    Launch the 'main' event loop, which will render frames until idle and then handle
    potential message requests.
     */
    return applicationScope.launch {
        while (isActive) {
            delay(64.milliseconds) // <- progressing the application every 64ms
            test.waitForIdle()

            val message = messages.tryReceive().getOrNull()

            if (message != null) {
                logger.debug("Received: $message")
            }

            if (message is TakeScreenshotRequest) {
                test.waitForIdle()
                delay(128.milliseconds)
                test.runOnUiThread {
                    val baos = ByteArrayOutputStream()
                    ImageIO.write(test.captureToImage().toAwtImage(), "png", baos)
                    orchestration.sendMessage(OrchestrationMessage.Screenshot("png", baos.toByteArray()))
                    logger.debug("Screenshot sent")
                }
            }

            if (message is ShutdownRequest) {
                applicationScope.coroutineContext.job.cancelChildren()
            }
        }
    }
}
