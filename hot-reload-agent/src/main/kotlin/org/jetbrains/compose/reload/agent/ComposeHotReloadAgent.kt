package org.jetbrains.compose.reload.agent

import javassist.ClassPool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent.reloadLock
import java.lang.instrument.ClassDefinition
import java.lang.instrument.Instrumentation
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes

private val logger = createLogger()

object ComposeHotReloadAgent {

    internal val _port = MutableStateFlow<Int?>(null)
    val port: StateFlow<Int?> = _port.asStateFlow()

    val reloadLock = ReentrantLock()

    private val beforeReloadListeners = mutableListOf<() -> Unit>()
    private val afterReloadListeners = mutableListOf<(error: Throwable?) -> Unit>()

    internal val pendingChanges = mutableMapOf<Path, ChangeType>()

    @Volatile
    private var instrumentation: Instrumentation? = null

    fun invokeBeforeReload(block: () -> Unit) = reloadLock.withLock {
        beforeReloadListeners.add(block)
    }

    fun invokeAfterReload(block: (error: Throwable?) -> Unit) = reloadLock.withLock {
        afterReloadListeners.add(block)
    }

    internal fun executeBeforeReloadListeners() = reloadLock.withLock {
        beforeReloadListeners.forEach { it() }
    }

    internal fun executeAfterReloadListeners(error: Throwable?) = reloadLock.withLock {
        afterReloadListeners.forEach { it(error) }
    }

    fun retryPendingChanges() {
        thread {
            reloadLock.withLock {
                executeBeforeReloadListeners()
                reload(instrumentation ?: return@thread)
            }
        }
    }

    @JvmStatic
    fun premain(args: String?, instrumentation: Instrumentation) {
        this.instrumentation = instrumentation
        enableComposeHotReloadMode()
        startServer(instrumentation)
    }
}

private fun startServer(instrumentation: Instrumentation) {
    thread(name = "Compose Hot Reload Agent / Server", isDaemon = true) {
        val socket = ServerSocket()
        socket.bind(null)

        logger.debug("Compose Hot Reload Agent: Listening on port: ${socket.localPort}")
        ComposeHotReloadAgent._port.value = socket.localPort

        while (true) {
            try {
                val connection = socket.accept()
                ComposeHotReloadAgent.executeBeforeReloadListeners()
                handleHotReloadRequest(connection, instrumentation)
            } catch (t: Throwable) {
                logger.error("Error w/ request", t)
                ComposeHotReloadAgent.executeAfterReloadListeners(t)
            }
        }
    }
}

private fun handleHotReloadRequest(socket: Socket, instrumentation: Instrumentation) = reloadLock.withLock {
    val reload = socket.getInputStream().bufferedReader().readText()
    val changes = parseChanges(reload)
    ComposeHotReloadAgent.pendingChanges.putAll(changes)
    reload(instrumentation)
    logger.debug("Reload: $reload")
}

private fun reload(instrumentation: Instrumentation) = reloadLock.withLock {
    val definitions = ComposeHotReloadAgent.pendingChanges.mapNotNull { (path, change) ->
        if (change == ChangeType.Removed) {
            return@mapNotNull null
        }

        if (path.extension != "class") {
            logger.warn("$change: $path is not a class")
            return@mapNotNull null
        }

        if (!path.isRegularFile()) {
            logger.warn("$change: $path is not a regular file")
            return@mapNotNull null
        }

        logger.debug("Loading: $path")
        val code = path.readBytes()
        val clazz = ClassPool.getDefault().makeClass(code.inputStream())
        ClassDefinition(Class.forName(clazz.name), code)
    }

    val result = runCatching {
        instrumentation.redefineClasses(*definitions.toTypedArray())
    }

    if (result.isSuccess) {
        ComposeHotReloadAgent.pendingChanges.clear()
        resetComposeErrors()
    }

    ComposeHotReloadAgent.executeAfterReloadListeners(result.exceptionOrNull())
}

enum class ChangeType {
    Added,
    Modified,
    Removed;

    companion object {
        fun fromString(value: String): ChangeType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: error("Unknown change type: $value")
        }
    }
}

fun parseChanges(raw: String): Map<Path, ChangeType> {
    val regex = Regex("""\[(.*)]\s(.*)""")
    return raw.lines().filter { it.isNotBlank() }.associate { line ->
        val match = regex.matchEntire(line) ?: error("Illegal instruction: '$line'")
        Path(match.groupValues[2]) to ChangeType.fromString(match.groupValues[1])
    }
}

