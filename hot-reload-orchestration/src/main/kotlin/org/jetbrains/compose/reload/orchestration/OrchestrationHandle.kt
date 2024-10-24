package org.jetbrains.compose.reload.orchestration

import java.util.concurrent.Future


public interface OrchestrationHandle : AutoCloseable {
    public val port: Int
    public fun invokeWhenClosed(action: () -> Unit)
    public fun invokeWhenMessageReceived(action: (OrchestrationMessage) -> Unit): Disposable
    public fun sendMessage(message: OrchestrationMessage): Future<Unit>
}

public inline fun <reified T> OrchestrationHandle.invokeWhenReceived(crossinline action: (T) -> Unit): Disposable {
    return invokeWhenMessageReceived { message ->
        if (message is T) action(message)
    }
}

public fun interface Disposable {
    public fun dispose()
}
