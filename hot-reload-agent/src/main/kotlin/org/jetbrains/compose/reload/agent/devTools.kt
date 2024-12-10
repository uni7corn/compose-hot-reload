package org.jetbrains.compose.reload.agent

import org.jetbrains.compose.reload.core.createLogger
import java.io.File
import kotlin.jvm.optionals.getOrNull

private val logger = createLogger()

internal fun launchDevtoolsApplication() {
    val classpath = (System.getProperty("compose.reload.devToolsClasspath") ?: error("mi")).split(File.pathSeparator)
    val java = ProcessHandle.current().info().command().getOrNull() ?: return

    logger.info("Starting Dev Tools")

    val process = ProcessBuilder(
        java, "-cp", classpath.joinToString(File.pathSeparator),
        "-Dcompose.reload.orchestration.port=${ComposeHotReloadAgent.orchestration.port}",
        "org.jetbrains.compose.reload.jvm.tooling.Main",
    ).inheritIO().start()

    Runtime.getRuntime().addShutdownHook(Thread {
        process.destroy()
    })
}
