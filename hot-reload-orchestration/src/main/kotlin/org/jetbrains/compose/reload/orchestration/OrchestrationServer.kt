package org.jetbrains.compose.reload.orchestration

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock


public interface OrchestrationServer : OrchestrationHandle

public fun startOrchestrationServer(): OrchestrationServer {
    val serverSocket = ServerSocket()
    serverSocket.bind(null)

    val logger = LoggerFactory.getLogger("OrchestrationServer(${serverSocket.localPort})")
    logger.debug("listening on port: ${serverSocket.localPort}")

    val server = OrchestrationServerImpl(serverSocket, orchestrationThread, logger)
    server.start()

    return server
}

private class OrchestrationServerImpl(
    private val serverSocket: ServerSocket,
    private val orchestrationThread: ExecutorService,
    private val logger: Logger
) : OrchestrationServer {
    private val isClosed = AtomicBoolean(false)
    private val isActive get() = !isClosed.get()
    private val lock = ReentrantLock()
    private val listeners = mutableListOf<(OrchestrationMessage) -> Unit>()
    private val closeListeners = mutableListOf<() -> Unit>()
    private val clients = mutableListOf<Client>()

    private class Client(
        private val socket: Socket,
    ) : AutoCloseable {
        val input by lazy { ObjectInputStream(socket.inputStream.buffered()) }
        val output by lazy { ObjectOutputStream(socket.outputStream.buffered()) }

        override fun close() {
            socket.close()
        }

        override fun toString(): String {
            return "Client(${socket.remoteSocketAddress})"
        }
    }

    override val port: Int
        get() = serverSocket.localPort

    override fun invokeWhenMessageReceived(action: (OrchestrationMessage) -> Unit): Disposable {
        val registration = Throwable()

        val safeListener: (OrchestrationMessage) -> Unit = { message ->
            try {
                action(message)
            } catch (t: Throwable) {
                logger.error("Failed invoking orchestration listener", t)
                logger.error("Failing listener was registered at:", registration)
            }
        }
        lock.withLock { listeners.add(safeListener) }
        return Disposable {
            lock.withLock { listeners.remove(safeListener) }
        }
    }

    override fun invokeWhenClosed(action: () -> Unit) {
        lock.withLock {
            if (isClosed.get()) action()
            else closeListeners.add(action)
        }
    }

    override fun sendMessage(message: OrchestrationMessage): Future<Unit> = orchestrationThread.submit<Unit> {
        val clients = lock.withLock { clients.toList() }

        clients.forEach { client ->
            try {
                client.output.writeObject(message)
                client.output.flush()
            } catch (_: Throwable) {
                logger.debug("Closing client: '$client'")
                client.close()
                lock.withLock {
                    this.clients.remove(client)
                }
            }
        }
    }

    fun start() {
        thread(name = "Orchestration Server", isDaemon = true) {
            while (isActive) {
                try {
                    val clientSocket = serverSocket.accept()
                    val client = Client(clientSocket)
                    logger.debug("Client connected: '$client'")
                    lock.withLock { clients.add(Client(clientSocket)) }
                    startReadingIncomingMessages(client)
                } catch (t: IOException) {
                    if (isActive) {
                        logger.warn("Server Socket exception", t)
                        close()
                    }
                }
            }
        }
    }

    private fun startReadingIncomingMessages(client: Client) {
        thread(name = "Orchestration Client Reader") {
            val stream = try {
                client.input
            } catch (_: IOException) {
                logger.warn("Client disconnected early: '$client'")
                client.close()
                return@thread
            }

            while (isActive) {
                val message = try {
                    stream.readObject()
                } catch (_: IOException) {
                    logger.debug("Client disconnected: '$client'")
                    lock.withLock { clients.remove(client) }
                    client.close()
                    break
                }
                if (message !is OrchestrationMessage) {
                    logger.debug("Unknown message received '$message'")
                    continue
                }

                logger.debug(
                    "Received message: ${message.javaClass.simpleName} " +
                            "'$client': '${message.messageId}'"
                )

                /* Notify orchestration thread about the message */
                orchestrationThread.submit {
                    val listeners = lock.withLock { listeners.toList() }
                    listeners.forEach { listener -> listener(message) }
                    // Broadcasting the message to all clients (including the one it came from)
                    sendMessage(message)
                }.get()
            }
        }
    }

    override fun close() {
        if (isClosed.getAndSet(true)) return
        orchestrationThread.submit {
            lock.withLock {
                logger.debug("Closing socket: '${serverSocket.localPort}'")
                clients.forEach { it.close() }
                clients.clear()
                serverSocket.close()
                closeListeners.forEach { it.invoke() }
                closeListeners.clear()
            }
        }
    }
}
