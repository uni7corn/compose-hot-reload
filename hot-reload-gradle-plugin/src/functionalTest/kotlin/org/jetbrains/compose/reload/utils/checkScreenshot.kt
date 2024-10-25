package org.jetbrains.compose.reload.utils

import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.Screenshot
import kotlin.io.path.*
import kotlin.test.fail

suspend fun HotReloadTestFixture.checkScreenshot(name: String) {
    orchestration.sendMessage(OrchestrationMessage.TakeScreenshotRequest())
    val screenshot = skipToMessage<Screenshot>()

    val directory = Path("src/functionalTest/resources/screenshots")
        .resolve(testClassName.asFileName().replace(".", "/"))
        .resolve(testMethodName.asFileName())

    val screenshotName = "$name.${screenshot.format}"
    val expectFile = directory.resolve(screenshotName)

    if (!expectFile.exists()) {
        expectFile.createParentDirectories()
        expectFile.writeBytes(screenshot.data)
        fail("Screenshot '$expectFile' did not exist; Generated")
    }

    if (!expectFile.readBytes().contentEquals(screenshot.data)) {
        val actualFile = expectFile.resolveSibling("${expectFile.nameWithoutExtension}-actual.${screenshot.format}")
        actualFile.writeBytes(screenshot.data)

        fail("Screenshot '${expectFile.pathString}' does not match")
    }
}

private fun String.asFileName(): String {
    return replace("""\\W+""", "_")
}