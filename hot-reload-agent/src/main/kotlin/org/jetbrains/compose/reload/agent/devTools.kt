package org.jetbrains.compose.reload.agent

import org.jetbrains.compose.reload.core.HotReloadEnvironment
import org.jetbrains.compose.reload.core.HotReloadProperty
import org.jetbrains.compose.reload.core.HotReloadProperty.DevToolsClasspath
import org.jetbrains.compose.reload.core.createLogger
import java.io.File
import kotlin.jvm.optionals.getOrNull

private val logger = createLogger()

internal fun launchDevtoolsApplication() {
    if (HotReloadEnvironment.isHeadless) return
    if (!HotReloadEnvironment.devToolsEnabled) return

    val classpath = HotReloadEnvironment.devToolsClasspath ?: error("Missing '${DevToolsClasspath}'")
    val java = ProcessHandle.current().info().command().getOrNull() ?: return

    logger.info("Starting Dev Tools")

    val process = ProcessBuilder(
        java, "-cp", classpath.joinToString(File.pathSeparator),
        "-D${HotReloadProperty.OrchestrationPort.key}=${ComposeHotReloadAgent.orchestration.port}",
        "-D${HotReloadProperty.GradleBuildContinuous.key}=${HotReloadEnvironment.gradleBuildContinuous}",
        "-Dapple.awt.UIElement=true",
        "org.jetbrains.compose.reload.jvm.tooling.Main",
    ).inheritIO().start()

    Runtime.getRuntime().addShutdownHook(Thread {
        process.destroy()
    })
}
