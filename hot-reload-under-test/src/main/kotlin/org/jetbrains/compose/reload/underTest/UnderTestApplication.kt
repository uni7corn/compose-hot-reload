@file:OptIn(ExperimentalTestApi::class)

package org.jetbrains.compose.reload.underTest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.window.singleWindowApplication
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.jetbrains.compose.reload.orchestration.OrchestrationClient
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ShutdownRequest
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.TakeScreenshotRequest
import org.jetbrains.compose.reload.orchestration.asChannel
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.lang.invoke.MethodHandles
import javax.imageio.ImageIO
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

private val applicationScope = CoroutineScope(Dispatchers.Default.limitedParallelism(1) + SupervisorJob())
private val orchestration = OrchestrationClient() ?: error("Failed connecting to orchestration")
private val messages = orchestration.asChannel()

@Suppress("unused") // Used by integration tests
fun underTestApplication(
    timeout: Int = 5,
    content: @Composable () -> Unit
) {
    if (System.getProperty("compose.reload.headless")?.toBoolean() == true) {
        runHeadless(timeout, content)
        return
    }

    /* Fallback: Actually start a real application */
    singleWindowApplication(title = "Application Under Test") {
        LaunchedEffect(Unit) {
            delay(timeout.minutes)
            exitProcess(-1)
        }

        DevelopmentEntryPoint(content)
    }
}

private fun runHeadless(timeout: Int = 5, content: @Composable () -> Unit) {
    runDesktopComposeUiTest(width = 256, height = 256) {
        setContent {
            Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                DevelopmentEntryPoint(content)
            }
        }

        launchHeadlessOrchestration(timeout.minutes, this)
            .asCompletableFuture()
            .join()
    }
}

private fun launchHeadlessOrchestration(timeout: Duration, test: SkikoComposeUiTest): Job {
    applicationScope.launch {
        delay(timeout)
        logger.error("Application timed out...")
        exitProcess(-1)
    }

    return applicationScope.launch {
        while (isActive) {
            delay(64.milliseconds) // <- progressing the application every 64ms
            test.waitForIdle()

            val message = messages.tryReceive().getOrNull()

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
