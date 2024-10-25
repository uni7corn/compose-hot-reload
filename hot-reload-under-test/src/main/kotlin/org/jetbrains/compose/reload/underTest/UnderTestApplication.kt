@file:OptIn(ExperimentalTestApi::class)

package org.jetbrains.compose.reload.underTest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.window.singleWindowApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.asFlow
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes

internal val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())


/**
 * Handle for code written 'under test'
 * Example:
 *
 * ```kotlin
 * underTestApplication {
 *     var state by remember { mutableStateOf(0) }
 *     onTestEvent { // <- can nicely call into such APIs without additional imports.
 *         state++
 *     }
 *     Text("Before: ${d}state", fontSize = 48.sp)
 * }
 * ```
 */
object UnderTestApplication {
    @Composable
    fun onTestEvent(handler: suspend (payload: Any?) -> Unit) {
        LaunchedEffect(Unit) {
            orchestration.asFlow().filterIsInstance<OrchestrationMessage.TestEvent>().collect { value ->
                logger.debug("TestEvent received: $value")
                handler.invoke(value.payload)
            }
        }
    }
}

/**
 * Entry points for "Applications under test"
 */
@Suppress("unused") // Used by integration tests
fun underTestApplication(
    timeout: Int = 5,
    content: @Composable UnderTestApplication.() -> Unit
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

        DevelopmentEntryPoint {
            UnderTestApplication.content()
        }
    }
}
