package org.jetbrains.compose.reload.utils

import org.gradle.api.logging.Logging
import org.intellij.lang.annotations.Language
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.*
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.nameWithoutExtension
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
    writeCode(source = source)
    launchApplicationAndWait()
}

suspend fun ScreenshotTestFixture.launchApplicationAndWait(
    projectPath: String = ":",
    mainClass: String = "MainKt",
) {
    hotReloadTestFixture.launchApplication(projectMode, projectPath, mainClass)

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
    replaceSourceCodeAndReload(sourceFile = getDefaultMainKtSourceFile(), oldValue, newValue)
}

suspend fun ScreenshotTestFixture.replaceSourceCodeAndReload(
    sourceFile: String = getDefaultMainKtSourceFile(),
    oldValue: String, newValue: String
) {
    val resolvedFile = hotReloadTestFixture.projectDir.resolve(sourceFile)
    reloadSourceCode(sourceFile, resolvedFile.readText().replace(oldValue, newValue))
}

suspend fun ScreenshotTestFixture.reloadSourceCode(
    sourceFile: String = getDefaultMainKtSourceFile(),
    source: String,
) {
    writeCode(sourceFile, source)

    logger.quiet("Waiting for reload request")
    val reloadRequest = run {
        val reloadRequest = hotReloadTestFixture.skipToMessage<ReloadClassesRequest>()
        if (reloadRequest.changedClassFiles.isEmpty()) fail("No changedClassFiles in reload request")
        if (reloadRequest.changedClassFiles.size > 1) fail("Too many changedClassFiles in reload request: ${reloadRequest.changedClassFiles}")
        val (requestedFile, changeType) = reloadRequest.changedClassFiles.entries.single()
        val sourceFileName = hotReloadTestFixture.projectDir.resolve(sourceFile).nameWithoutExtension
        requestedFile.name.assertMatchesRegex(""".*${sourceFileName}Kt.*\.class""")
        assertEquals(ReloadClassesRequest.ChangeType.Modified, changeType)
        reloadRequest
    }

    logger.quiet("Waiting for UI render")
    run {
        val rendered = hotReloadTestFixture.skipToMessage<UIRendered>()
        assertEquals(reloadRequest.messageId, rendered.reloadRequestId)
    }
}

private fun ScreenshotTestFixture.writeCode(
    sourceFile: String = getDefaultMainKtSourceFile(),
    @Language("kotlin") source: String
) {
    val resolvedFile = hotReloadTestFixture.projectDir.resolve(sourceFile)
    resolvedFile.createParentDirectories()
    resolvedFile.writeText(source)
}

private fun ScreenshotTestFixture.getDefaultMainKtSourceFile(): String {
    return when (projectMode) {
        ProjectMode.Kmp -> "src/commonMain/kotlin/Main.kt"
        ProjectMode.Jvm -> "src/main/kotlin/Main.kt"
    }
}


