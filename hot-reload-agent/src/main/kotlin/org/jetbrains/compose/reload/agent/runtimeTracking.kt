package org.jetbrains.compose.reload.agent

import org.jetbrains.compose.reload.analysis.RuntimeInfo
import org.jetbrains.compose.reload.analysis.RuntimeScopeInfo
import org.jetbrains.compose.reload.core.Update
import org.jetbrains.compose.reload.core.createLogger
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.concurrent.thread

private val logger = createLogger()

private val runtimeAnalysisThread = Executors.newSingleThreadExecutor { r ->
    thread(start = false, isDaemon = true, name = "Compose Runtime Analyzer", block = r::run)
}

private val currentRuntime = ArrayList<RuntimeScopeInfo>(16384)
private val pendingRedefinitions = ArrayList<RuntimeScopeInfo>(16384)

internal fun redefineRuntimeInfo(): Future<Update<RuntimeInfo>> = runtimeAnalysisThread.submit<Update<RuntimeInfo>> {
    val previousRuntimeScopes = currentRuntime.toList()

    // Patch method ids
    val pendingMethodIds = pendingRedefinitions.mapTo(hashSetOf()) { info -> info.methodId }
    currentRuntime.removeAll { info -> info.methodId in pendingMethodIds }
    currentRuntime.addAll(pendingRedefinitions)
    pendingRedefinitions.clear()

    Update(
        RuntimeInfo(previousRuntimeScopes),
        RuntimeInfo(currentRuntime.toList())
    )
}

internal fun enqueueRuntimeAnalysis(
    className: String?, classBeingRedefined: Class<*>?, classfileBuffer: ByteArray
) = runtimeAnalysisThread.submit {
    try {
        val classInfo = RuntimeInfo(classfileBuffer) ?: return@submit

        if (classBeingRedefined == null) {
            if (logger.isTraceEnabled) {
                logger.trace("Parsed 'RuntimeInfo' for '$className'")
            }
            currentRuntime.addAll(classInfo.scopes)
        } else {
            if (logger.isTraceEnabled) {
                logger.trace("Parsed 'RuntimeInfo' for '$className' (redefined)")
            }
            pendingRedefinitions.addAll(classInfo.scopes)
        }

    } catch (t: Throwable) {
        logger.error("Failed parsing 'RuntimeInfo' for '$className'", t)
    }
}
