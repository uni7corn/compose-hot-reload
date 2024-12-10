package org.jetbrains.compose.reload.agent

import org.jetbrains.compose.reload.core.createLogger
import org.jetbrains.compose.reload.orchestration.Disposable
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ReloadClassesRequest
import java.lang.instrument.Instrumentation
import java.util.*


object ComposeHotReloadAgent {

    internal val logger = createLogger()

    private val beforeReloadListeners = mutableListOf<(reloadRequestId: UUID) -> Unit>()
    private val afterReloadListeners = mutableListOf<(reloadRequestId: UUID, error: Throwable?) -> Unit>()


    @Volatile
    private var instrumentation: Instrumentation? = null

    val orchestration by lazy { startOrchestration() }

    fun invokeBeforeReload(block: (reloadRequestId: UUID) -> Unit): Disposable = synchronized(beforeReloadListeners) {
        beforeReloadListeners.add(block)
        Disposable {
            synchronized(beforeReloadListeners) {
                beforeReloadListeners.remove(block)
            }
        }
    }

    fun invokeAfterReload(block: (reloadRequestId: UUID, error: Throwable?) -> Unit): Disposable =
        synchronized(afterReloadListeners) {
            afterReloadListeners.add(block)
            Disposable {
                synchronized(afterReloadListeners) {
                    afterReloadListeners.remove(block)
                }
            }
        }

    internal fun executeBeforeReloadListeners(reloadRequestId: UUID) {
        val listeners = synchronized(beforeReloadListeners) { beforeReloadListeners.toList() }
        listeners.forEach { listener -> listener(reloadRequestId) }
    }

    internal fun executeAfterReloadListeners(reloadRequestId: UUID, error: Throwable?) {
        val listeners = synchronized(afterReloadListeners) { afterReloadListeners.toList() }
        listeners.forEach { listener -> listener(reloadRequestId, error) }
    }

    fun retryPendingChanges() {
        orchestration.sendMessage(ReloadClassesRequest())
    }

    @JvmStatic
    fun premain(args: String?, instrumentation: Instrumentation) {
        this.instrumentation = instrumentation
        enableComposeHotReloadMode()
        startComposeGroupInvalidationTransformation(instrumentation)
        launchReloadClassesRequestHandler(instrumentation)
        launchRecompiler()

        ComposeReloadPremainExtension.load().forEach { extension ->
            extension.premain()
        }
    }
}
