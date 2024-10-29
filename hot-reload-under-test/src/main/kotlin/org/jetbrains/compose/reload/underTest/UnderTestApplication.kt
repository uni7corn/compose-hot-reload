@file:OptIn(ExperimentalTestApi::class)

package org.jetbrains.compose.reload.underTest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.ExperimentalTestApi
import kotlinx.coroutines.flow.filterIsInstance
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent.orchestration
import org.jetbrains.compose.reload.InternalHotReloadApi
import org.jetbrains.compose.reload.jvm.runDevApplicationHeadless
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.asFlow
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
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
@OptIn(InternalHotReloadApi::class)
@Suppress("unused") // Used by integration tests
fun underTestApplication(
    timeout: Int = 5,
    content: @Composable UnderTestApplication.() -> Unit
) {
    runDevApplicationHeadless(timeout.minutes, width = 256, height = 256) {
        DevelopmentEntryPoint {
            UnderTestApplication.content()
        }
    }
}
