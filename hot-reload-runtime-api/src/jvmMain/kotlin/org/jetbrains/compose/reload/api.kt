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
            return org.jetbrains.compose.reload.jvm.invokeAfterHotReload(action)
        }
    }

@Composable
public actual fun AfterHotReloadEffect(action: () -> Unit) {
    if (!isHotReloadActive) return
    LaunchedEffect(Unit) {
        val actionJob = Job(coroutineContext[Job])

        val registration = org.jetbrains.compose.reload.jvm.invokeAfterHotReload {
            try {
                if (actionJob.isActive) {
                    action()
                }
            }
            // https://github.com/JetBrains/compose-hot-reload/issues/66
            catch (_: NoSuchMethodError) {
                actionJob.complete()
            }
            catch (t: Throwable) {
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
