package org.jetbrains.compose.reload.agent

import org.jetbrains.compose.reload.orchestration.Disposable
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ReloadClassesRequest
import java.lang.instrument.Instrumentation
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


object ComposeHotReloadAgent {

    internal val logger = createLogger()
    val reloadLock = ReentrantLock()

    private val beforeReloadListeners = mutableListOf<(reloadRequestId: UUID) -> Unit>()
    private val afterReloadListeners = mutableListOf<(reloadRequestId: UUID, error: Throwable?) -> Unit>()


    @Volatile
    private var instrumentation: Instrumentation? = null

    val orchestration by lazy { startOrchestration() }

    fun invokeBeforeReload(block: (reloadRequestId: UUID) -> Unit): Disposable = reloadLock.withLock {
        beforeReloadListeners.add(block)
        Disposable {
            reloadLock.withLock {
                beforeReloadListeners.remove(block)
            }
        }
    }

    fun invokeAfterReload(block: (reloadRequestId: UUID, error: Throwable?) -> Unit): Disposable = reloadLock.withLock {
        afterReloadListeners.add(block)
        Disposable {
            reloadLock.withLock {
                afterReloadListeners.remove(block)
            }
        }
    }

    internal fun executeBeforeReloadListeners(reloadRequestId: UUID) = reloadLock.withLock {
        beforeReloadListeners.forEach { it(reloadRequestId) }
    }

    internal fun executeAfterReloadListeners(reloadRequestId: UUID, error: Throwable?) = reloadLock.withLock {
        afterReloadListeners.forEach { it(reloadRequestId, error) }
    }

    fun retryPendingChanges() {
        orchestration.sendMessage(ReloadClassesRequest())
    }

    @JvmStatic
    fun premain(args: String?, instrumentation: Instrumentation) {
        this.instrumentation = instrumentation
        enableComposeHotReloadMode()
        launchReloadClassesRequestHandler(instrumentation)
        launchRecompiler()

        ComposeReloadPremainExtension.load().forEach { extension ->
            extension.premain()
        }
    }
}
