package org.jetbrains.compose.reload.agent

import javassist.ClassPool
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import java.io.File
import java.lang.instrument.ClassDefinition
import java.lang.instrument.Instrumentation
import kotlin.concurrent.withLock

internal fun reload(
    instrumentation: Instrumentation, pendingChanges: Map<File, OrchestrationMessage.ReloadClassesRequest.ChangeType>
) = ComposeHotReloadAgent.reloadLock.withLock {
    val definitions = pendingChanges.mapNotNull { (file, change) ->
        if (change == OrchestrationMessage.ReloadClassesRequest.ChangeType.Removed) {
            return@mapNotNull null
        }

        if (file.extension != "class") {
            logger.warn("$change: $file is not a class")
            return@mapNotNull null
        }

        if (!file.isFile) {
            logger.warn("$change: $file is not a regular file")
            return@mapNotNull null
        }

        logger.debug("Loading: $file")
        val code = file.readBytes()
        val clazz = ClassPool.getDefault().makeClass(code.inputStream())
        ClassDefinition(Class.forName(clazz.name), code)
    }

    instrumentation.redefineClasses(*definitions.toTypedArray())
}