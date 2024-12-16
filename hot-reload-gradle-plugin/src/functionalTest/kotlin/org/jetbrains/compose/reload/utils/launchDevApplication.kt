package org.jetbrains.compose.reload.utils

import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.RecompilerReady
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.UIRendered

suspend fun HotReloadTestFixture.launchDevApplicationAndWait(
    projectPath: String = ":",
    className: String,
    funName: String
) {
    launchDevApplication(projectPath, className, funName)
    skipToMessage<UIRendered>()
    skipToMessage<RecompilerReady>()
}

suspend fun HotReloadTestFixture.launchDevApplication(
    projectPath: String = ":",
    className: String,
    funName: String
) {
    launchDaemonThread {
        val runTaskPath = buildString {
            if (projectPath != ":") {
                append(projectPath)
            }

            append(":")
            append("devRun")
        }

        gradleRunner.addedArguments(
            "wrapper", runTaskPath, "-DclassName=$className", "-DfunName=$funName"
        ).build()
    }
}
