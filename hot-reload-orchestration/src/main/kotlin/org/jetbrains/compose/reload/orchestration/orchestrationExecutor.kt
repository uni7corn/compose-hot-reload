package org.jetbrains.compose.reload.orchestration

import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

private var orchestrationThread = AtomicReference<Thread?>(null)

internal val orchestrationExecutor = Executors.newSingleThreadExecutor { runnable ->
    val newThread = thread(
        name = "Orchestration Main",
        start = false,
        isDaemon = true,
        block = { runnable.run() }
    )

    check(orchestrationThread.getAndSet(newThread) == null)
    newThread
}

public fun isOrchestrationThread(): Boolean = Thread.currentThread() == orchestrationThread.get()

public fun checkIsOrchestrationThread(): Unit = check(isOrchestrationThread()) {
    "Required Orchestration Thread '${orchestrationThread.get()?.name}', but was called on ${Thread.currentThread().name}"
}

public fun <T> runInOrchestrationThread(action: () -> T): Future<T> = orchestrationExecutor.submit<T> {
    action()
}

public fun <T> runInOrchestrationThreadBlocking(action: () -> T): T {
    if (isOrchestrationThread()) return action()
    return runInOrchestrationThread(action).get()
}
