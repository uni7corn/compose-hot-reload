/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.compose.devtools

import org.jetbrains.compose.reload.core.HotReloadEnvironment
import org.jetbrains.compose.reload.core.createLogger
import kotlin.concurrent.thread
import kotlin.jvm.optionals.getOrNull
import kotlin.system.exitProcess

private val logger = createLogger()

internal fun setupDevToolsProcess() {
    setupOrchestration()
    bindParentProcess()
}

private fun bindParentProcess() {
    val pid = HotReloadEnvironment.parentPid ?: run {
        logger.error("parentPid: Missing")
        shutdown()
    }

    val process = ProcessHandle.of(pid).getOrNull() ?: run {
        logger.error("parentPid: Cannot find process with pid=$pid")
        shutdown()
    }

    thread {
        process.onExit().get()
        logger.info("parentPid: Parent process with pid=$pid exited")
        shutdown()
    }
}

private fun setupOrchestration() {
    orchestration.invokeWhenClosed {
        shutdown()
    }
}

internal fun shutdown(): Nothing {
    exitProcess(0)
}
