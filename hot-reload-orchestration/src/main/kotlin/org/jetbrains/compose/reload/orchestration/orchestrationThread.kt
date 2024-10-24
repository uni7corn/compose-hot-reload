package org.jetbrains.compose.reload.orchestration

import java.util.concurrent.Executors
import kotlin.concurrent.thread

internal val orchestrationThread = Executors.newSingleThreadExecutor { runnable ->
    thread(
        name = "Orchestration Main",
        start = false,
        isDaemon = true,
        block = { runnable.run() }
    )
}