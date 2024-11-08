package org.jetbrains.compose.reload.orchestration

import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ClientConnected
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import java.util.concurrent.CompletableFuture
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

    private inner class Client(
        private val socket: Socket,
        val id: UUID,
        val role: OrchestrationClientRole,
        val input: ObjectInputStream,
        val output: ObjectOutputStream
    ) : AutoCloseable {

        override fun close() {
            socket.close()
            sendMessage(OrchestrationMessage.ClientDisconnected(id, role))
        }

        override fun toString(): String {
            return "Client [$role] (${socket.remoteSocketAddress})"
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
        /* Send the message to all, currently connected clients */
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

        /*
        Send the message to all message listeners:
        Clients will get an 'echo' from the server, once the server has handled the message.
        When the server sends a message, the 'echo' will be sent once all clients received the message.
         */
        invokeMessageListeners(message)
    }

    fun start() {
        thread(name = "Orchestration Server", isDaemon = true) {
            while (isActive) {
                try {
                    val clientSocket = serverSocket.accept()
                    startClient(clientSocket)
                } catch (t: IOException) {
                    if (isActive) {
                        logger.warn("Server Socket exception", t)
                        close()
                    }
                }
            }
        }
    }

    private fun startClient(socket: Socket) = thread(name = "Orchestration Client Reader") {
        /* Read Handshake and create the 'client' object */
        val client = try {
            logger.debug("Socket connected: '${socket.remoteSocketAddress}'")
            val input = ObjectInputStream(socket.getInputStream().buffered())
            val output = ObjectOutputStream(socket.getOutputStream().buffered())
            val handshake = input.readObject() as OrchestrationHandshake

            val client = Client(socket, handshake.clientId, handshake.clientRole, input, output)
            logger.debug("Client connected: '$client'")
            lock.withLock { clients.add(client) }
            client
        } catch (t: Throwable) {
            logger.debug("Client cannot be connected: '${socket.remoteSocketAddress}'")
            logger.trace("Client cannot be connected: '${socket.remoteSocketAddress}'", t)
            socket.close()
            return@thread
        }

        /* Announce the new client to the whole orchestration */
        sendMessage(ClientConnected(client.id, client.role))

        /* Read messages  */
        while (isActive) {
            val message = try {
                client.input.readObject()
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

            /* Broadcasting the message to all clients (including the one it came from) */
            sendMessage(message).get()
        }
    }

    /**
     * Expected to be called from the [orchestrationThread];
     * Will invoke all listeners
     */
    private fun invokeMessageListeners(message: OrchestrationMessage) {
        val listeners = lock.withLock { listeners.toList() }
        listeners.forEach { listener -> listener(message) }
    }

    override fun close() {
        closeGracefully()
    }

    override fun closeGracefully(): Future<Unit> {
        if (isClosed.getAndSet(true)) CompletableFuture.completedFuture(Unit)

        val finished = CompletableFuture<Unit>()
        orchestrationThread.submit {
            lock.withLock {
                try {
                    logger.debug("Closing socket: '${serverSocket.localPort}'")
                    clients.forEach { it.close() }
                    clients.clear()
                    serverSocket.close()
                    closeListeners.forEach { it.invoke() }
                    closeListeners.clear()
                } catch (t: Throwable) {
                    logger.warn("Failed closing server: '${serverSocket.localPort}'", t)
                } finally {
                    /* Send the 'finished' signal after all currently enqueued tasks have finished */
                    orchestrationThread.submit {
                        finished.complete(Unit)
                    }
                }
            }
        }
        return finished
    }

    override fun closeImmediately() {
        if (isClosed.getAndSet(true)) return

        lock.withLock {
            logger.debug("Closing socket (immediately): '${serverSocket.localPort}'")
            clients.forEach { it.close() }
            clients.clear()
            serverSocket.close()
            orchestrationThread.shutdownNow()
        }
    }
}
