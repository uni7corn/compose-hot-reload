package org.jetbrains.compose.reload.agent

import javassist.ClassPool
import javassist.CtClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.instrument.ClassDefinition
import java.lang.instrument.Instrumentation
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.io.path.Path
import kotlin.io.path.readBytes

private val logger = createLogger()

object ComposeHotReloadAgent {

    internal val _port = MutableStateFlow<Int?>(null)
    val port: StateFlow<Int?> = _port.asStateFlow()

    val reloadLock = ReentrantLock()

    private val beforeReloadListeners = mutableListOf<() -> Unit>()
    private val afterReloadListeners = mutableListOf<(error: Throwable?) -> Unit>()

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

    fun withReloadLock(block: () -> Unit) = reloadLock.withLock {
        block()
    }

    @JvmStatic
    fun premain(args: String?, instrumentation: Instrumentation) {
        listenForChanges(instrumentation)
    }
}

private fun listenForChanges(instrumentation: Instrumentation) {
    thread(name = "Compose Hot Reload Agent / Server", isDaemon = true) {
        val socket = ServerSocket()
        socket.bind(null)

        logger.debug("Compose Hot Reload Agent: Listening on port: ${socket.localPort}")
        ComposeHotReloadAgent._port.value = socket.localPort

        while (true) {
            try {
                val connection = socket.accept()
                withConnection(connection, instrumentation)
            } catch (t: Throwable) {
                logger.error("Error w/ connection", t)
            }
        }
    }
}

private fun withConnection(socket: Socket, instrumentation: Instrumentation) {
    val reload = socket.getInputStream().bufferedReader().readText()
    val changes = parseChanges(reload)


    val definitions = changes.mapNotNull { change ->
        if (change.key == ChangeType.Removed) {
            return@mapNotNull null
        }

        val code = change.value.readBytes()
        val clazz = ClassPool.getDefault().makeClass(code.inputStream())
        ClassDefinition(Class.forName(clazz.name), code)
    }

    ComposeHotReloadAgent.reloadLock.withLock {
        ComposeHotReloadAgent.executeBeforeReloadListeners()
        val result = runCatching {
            instrumentation.redefineClasses(*definitions.toTypedArray())
        }
        ComposeHotReloadAgent.executeAfterReloadListeners(result.exceptionOrNull())
    }

    logger.debug("Reload: $reload")
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

fun parseChanges(raw: String): Map<ChangeType, Path> {
    val regex = Regex("""\[(.*)]\s(.*)""")
    return raw.lines().filter { it.isNotBlank() }.associate { line ->
        val match = regex.matchEntire(line) ?: error("Illegal instruction: '$line'")
        ChangeType.fromString(match.groupValues[1]) to Path(match.groupValues[2])
    }
}

