package org.jetbrains.compose.reload.jvm.tooling.states

import io.sellmair.evas.State
import io.sellmair.evas.launchState
import io.sellmair.evas.update
import io.sellmair.evas.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.compose.reload.jvm.tooling.orchestration
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.asFlow
import kotlin.time.Duration.Companion.seconds

sealed class ReloadState : State {

    abstract val time: Instant

    data class Ok(
        override val time: Instant = Clock.System.now(),
    ) : ReloadState()

    data class Reloading(
        override val time: Instant = Clock.System.now(),
    ) : ReloadState()

    data class Failed(
        val reason: String,
        override val time: Instant = Clock.System.now(),
    ) : ReloadState()

    companion object Key : State.Key<ReloadState> {
        override val default: ReloadState = Ok(time = Clock.System.now())
    }
}

fun CoroutineScope.launchReloadState() = launchState(ReloadState) {
    orchestration.asFlow().collect { message ->
        if (message is OrchestrationMessage.ReloadClassesResult) {
            emit(
                if (message.isSuccess) ReloadState.Ok()
                else ReloadState.Failed("Failed reloading classes (${message.errorMessage})")
            )
        }

        if (message is OrchestrationMessage.ReloadClassesRequest) {
            ReloadState.update { state ->
                state as? ReloadState.Reloading ?: ReloadState.Reloading(time = Clock.System.now())
            }
        }

        if (message is OrchestrationMessage.LogMessage && message.tag == OrchestrationMessage.LogMessage.TAG_COMPILER) {
            if (message.message.contains("executing build...")) {
                ReloadState.Reloading(time = Clock.System.now()).emit()
            }

            if (message.message.contains("BUILD FAILED")) {
                ReloadState.Failed("Compilation Failed").emit()
            }

            /*
            The build was successful, but this does not necessarily mean that a reload request is fired.
            Example: Lets say some code was added, which causes a 'BUILD FAILED'.
            Afterward, the failing code was removed again. A 'BUILD SUCCESSFUL' will be observed w/o firing
            a reload request, because the runtime classpath is not changed to the last working state.

            Because of this, a new coroutine is launched after 'BUILD SUCCESSFUL' to only update
            the reload state to 'Ok' after a debounced delay of 1 second.
             */
            if (message.message.contains("BUILD SUCCESSFUL")) {
                val capturedState = ReloadState.value()
                launch {
                    delay(1.seconds)
                    ReloadState.update { currentState ->
                        if (currentState === capturedState) ReloadState.Ok(time = Clock.System.now())
                        else currentState
                    }
                }
            }
        }
    }
}
