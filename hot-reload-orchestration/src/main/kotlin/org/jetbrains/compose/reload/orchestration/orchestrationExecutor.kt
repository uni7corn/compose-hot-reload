package org.jetbrains.compose.reload.orchestration

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

private val orchestrationThreadReference = AtomicReference<Thread?>(null)

internal val orchestrationThread = Executors.newSingleThreadExecutor { runnable ->
    val newThread = thread(
        name = "Orchestration Main",
        start = false,
        isDaemon = true,
        block = { runnable.run() }
    )

    check(orchestrationThreadReference.getAndSet(newThread) == null)
    newThread
}

public fun isOrchestrationThread(): Boolean = Thread.currentThread() == orchestrationThreadReference.get()

public fun checkIsOrchestrationThread(): Unit = check(isOrchestrationThread()) {
    "Required Orchestration Thread '${orchestrationThreadReference.get()?.name}', but was called on ${Thread.currentThread().name}"
}

public fun <T> runInOrchestrationThreadBlocking(action: () -> T): T {
    if (isOrchestrationThread()) return action()
    return orchestrationThread.submit(action).get()
}
