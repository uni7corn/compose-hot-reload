package org.jetbrains.compose.reload.orchestration

import org.slf4j.LoggerFactory
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.InetAddress
import java.net.Socket
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock


public interface OrchestrationClient : OrchestrationHandle {
    public val clientId: UUID
}

public fun connectOrchestrationClient(role: OrchestrationClientRole, port: Int): OrchestrationClient {
    val socket = Socket(InetAddress.getLocalHost(), port)

    socket.keepAlive = true
    val client = OrchestrationClientImpl(role, socket, orchestrationThread, port)
    client.start()
    return client
}

private class OrchestrationClientImpl(
    private val role: OrchestrationClientRole,
    private val socket: Socket,
    private val orchestrationThread: ExecutorService,
    override val port: Int,
) : OrchestrationClient {

    override val clientId = UUID.randomUUID()
    private val logger = LoggerFactory.getLogger("OrchestrationClient(${socket.localPort})")
    private val lock = ReentrantLock()
    private val isClosed = AtomicBoolean(false)
    private val isActive get() = !isClosed.get()
    private val output = ObjectOutputStream(socket.getOutputStream().buffered())
    private val listeners = mutableListOf<(OrchestrationMessage) -> Unit>()
    private val closeListeners = mutableListOf<() -> Unit>()


    override fun invokeWhenMessageReceived(action: (OrchestrationMessage) -> Unit): Disposable {
        lock.withLock { listeners.add(action) }
        return Disposable {
            lock.withLock { listeners.remove(action) }
        }
    }

    override fun invokeWhenClosed(action: () -> Unit) {
        lock.withLock {
            if (isClosed.get()) action()
            else closeListeners.add(action)
        }
    }

    override fun sendMessage(message: OrchestrationMessage): Future<Unit> = orchestrationThread.submit<Unit> {
        try {
            output.writeObject(message)
            output.flush()
            logger.debug("Sent message: ${message.javaClass.simpleName} '${message.messageId}'")
        } catch (_: Throwable) {
            logger.debug("Sender: Closing client")
            close()
        }
    }

    fun start() {
        orchestrationThread.submit {
            try {
                output.writeObject(OrchestrationHandshake(clientId, role))
                output.flush()
            } catch (_: Throwable) {
                logger.debug("start: Closing client")
                close()
            }
        }

        thread(name = "Orchestration Client Reader") {
            logger.debug("connected")

            try {
                val input = ObjectInputStream(socket.getInputStream().buffered())

                while (isActive) {
                    val message = input.readObject()
                    if (message !is OrchestrationMessage) {
                        logger.debug("Unknown message received '$message'")
                        continue
                    }

                    /* Notify orchestration thread about the message */
                    orchestrationThread.submit {
                        val listeners = lock.withLock { listeners.toList() }
                        listeners.forEach { listener -> listener(message) }
                    }.get()
                }
            } catch (t: Throwable) {
                logger.debug("reader: closing client")
                logger.trace("reader: closed with traces", t)
                close()
            }
        }
    }

    override fun close() {
        closeGracefully()
    }

    override fun closeGracefully(): Future<Unit> {
        if (isClosed.getAndSet(true)) return CompletableFuture.completedFuture(Unit)
        val finished = CompletableFuture<Unit>()

        orchestrationThread.submit {
            try {
                logger.debug("Closing socket: '${socket.port}' ('${socket.localPort}')")
                socket.close()

                val closeListeners = lock.withLock {
                    try {
                        closeListeners.toList()
                    } finally {
                        closeListeners.clear()
                    }
                }

                closeListeners.forEach { listener ->
                    try {
                        listener.invoke()
                    } catch (t: Throwable) {
                        logger.error("Failed invoking close listener", t)
                    }
                }
            } finally {
                /* Send 'finished' signal when all currently enqueued tasks were completed */
                orchestrationThread.submit {
                    finished.complete(Unit)
                }
            }
        }

        return finished
    }

    override fun closeImmediately() {
        if (isClosed.getAndSet(true)) return
        logger.debug("Closing socket (immediately): '${socket.port}' ('${socket.localPort}')")
        socket.close()
    }
}
