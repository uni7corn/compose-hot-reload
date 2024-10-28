package org.jetbrains.compose.reload.utils

import org.gradle.api.logging.Logging
import org.intellij.lang.annotations.Language
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.*
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private val logger = Logging.getLogger("ScreenshotTestFixture")

class ScreenshotTestFixture(
    val projectMode: ProjectMode,
    val hotReloadTestFixture: HotReloadTestFixture
) {
    fun runTest(timeout: Duration = 1.minutes, test: suspend () -> Unit) =
        hotReloadTestFixture.runTest(timeout, test)
}

suspend fun ScreenshotTestFixture.checkScreenshot(name: String) {
    hotReloadTestFixture.checkScreenshot(name)
}

suspend infix fun ScreenshotTestFixture.initialSourceCode(source: String) {
    writeCode(source)

    hotReloadTestFixture.launchApplication(projectMode)

    logger.quiet("Waiting for UI to render")
    run {
        val rendered = hotReloadTestFixture.skipToMessage<UIRendered>()
        assertNull(rendered.reloadRequestId)
    }

    logger.quiet("Waiting for Daemon to become ready")
    hotReloadTestFixture.skipToMessage<GradleDaemonReady>()
}

suspend fun ScreenshotTestFixture.replaceSourceCodeAndReload(
    oldValue: String, newValue: String
) {
    val sourceFile = getSourceFile()
    reloadSourceCode(sourceFile.readText().replace(oldValue, newValue))
}

suspend infix fun ScreenshotTestFixture.reloadSourceCode(source: String) {
    writeCode(source)

    logger.quiet("Waiting for reload request")
    val reloadRequest = run {
        val reloadRequest = hotReloadTestFixture.skipToMessage<ReloadClassesRequest>()
        if (reloadRequest.changedClassFiles.isEmpty()) fail("No changedClassFiles in reload request")
        if (reloadRequest.changedClassFiles.size > 1) fail("Too many changedClassFiles in reload request: ${reloadRequest.changedClassFiles}")
        val (requestedFile, changeType) = reloadRequest.changedClassFiles.entries.single()
        requestedFile.name.assertMatchesRegex(""".*MainKt.*\.class""")
        assertEquals(ReloadClassesRequest.ChangeType.Modified, changeType)
        reloadRequest
    }

    logger.quiet("Waiting for UI render")
    run {
        val rendered = hotReloadTestFixture.skipToMessage<UIRendered>()
        assertEquals(reloadRequest.messageId, rendered.reloadRequestId)
    }
}


private infix fun ScreenshotTestFixture.writeCode(@Language("kotlin") source: String) {
    val sourceFile = getSourceFile()
    sourceFile.createParentDirectories()
    sourceFile.writeText(source)
}

private fun ScreenshotTestFixture.getSourceFile(): Path {
    return when (projectMode) {
        ProjectMode.Kmp -> hotReloadTestFixture.projectDir.resolve("src/commonMain/kotlin/Main.kt")
        ProjectMode.Jvm -> hotReloadTestFixture.projectDir.resolve("src/main/kotlin/Main.kt")
    }
}

