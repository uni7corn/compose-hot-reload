@file:JvmName("HotReloadApi")

package org.jetbrains.compose.reload

import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import org.jetbrains.compose.reload.jvm.isHotReloadActive


@OptIn(InternalComposeApi::class)
@Composable
public actual fun DevelopmentEntryPoint(child: @Composable () -> Unit) {
    org.jetbrains.compose.reload.jvm.DevelopmentEntryPoint(child)
}

@DelicateHotReloadApi
public actual val staticHotReloadScope: HotReloadScope =
    if (!isHotReloadActive) NoopHotReloadScope else object : HotReloadScope() {
        override fun invokeAfterHotReload(action: () -> Unit): AutoCloseable {
            /*
            Wrap the action to track the action's life:
            If the underlying 'action' code got removed, then we can release the listener.
            */

            val actionClassLifetimeToken = ClassLifetimeToken(action)
            var registration: AutoCloseable? = null
            registration = org.jetbrains.compose.reload.jvm.invokeAfterHotReload {
                /* Check if the action class is still alive: If not, close the registration */
                if (!actionClassLifetimeToken.isAlive()) {
                    registration?.close()
                    return@invokeAfterHotReload
                }

                action()
            }
            return registration
        }
    }

@Composable
public actual fun AfterHotReloadEffect(action: () -> Unit) {
    if (!isHotReloadActive) return
    val actionClassLifetimeToken = ClassLifetimeToken(action)

    LaunchedEffect(Unit) {
        val actionJob = Job(coroutineContext[Job])

        val registration = org.jetbrains.compose.reload.jvm.invokeAfterHotReload {
            try {
                /* Guard 1: If the job is marked as completed, we can return already */
                if (!actionJob.isActive) return@invokeAfterHotReload

                /* Guard 2: If the lambda class was removed, we can complete and return */

                if (!actionClassLifetimeToken.isAlive()) {
                    actionJob.complete()
                    return@invokeAfterHotReload
                }

                action()
            } catch (t: Throwable) {
                actionJob.completeExceptionally(t)
            }
        }

        coroutineContext.job.invokeOnCompletion { registration.close() }
        /*
         Joining the 'actionJob' will keep this coroutine alive until canceled, but also will
         forward the exception thrown in [action]
         */
        actionJob.job.join()
    }
}
