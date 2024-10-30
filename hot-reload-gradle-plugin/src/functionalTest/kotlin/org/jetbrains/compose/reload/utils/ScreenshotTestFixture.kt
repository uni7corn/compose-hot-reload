package org.jetbrains.compose.reload.utils

import org.intellij.lang.annotations.Language
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.*
import java.nio.file.Path
import kotlin.io.path.PathWalkOption
import kotlin.io.path.createParentDirectories
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail


suspend infix fun HotReloadTestFixture.initialSourceCode(source: String): Path {
    val file = writeCode(source = source)
    launchApplicationAndWait()
    return file
}

suspend fun HotReloadTestFixture.launchApplicationAndWait(
    projectPath: String = ":",
    mainClass: String = "MainKt",
) {
    launchApplication(projectPath, mainClass)

    logger.quiet("Waiting for UI to render")
    run {
        val rendered = skipToMessage<UIRendered>()
        assertNull(rendered.reloadRequestId)
    }

    logger.quiet("Waiting for Daemon to become ready")
    skipToMessage<GradleDaemonReady>()
}

suspend fun HotReloadTestFixture.replaceSourceCodeAndReload(
    oldValue: String, newValue: String
) {
    replaceSourceCodeAndReload(sourceFile = getDefaultMainKtSourceFile(), oldValue, newValue)
}

suspend fun HotReloadTestFixture.replaceSourceCodeAndReload(
    sourceFile: String = getDefaultMainKtSourceFile(),
    oldValue: String, newValue: String
) {
    val resolvedFile = projectDir.resolve(sourceFile)
    reloadSourceCode(sourceFile, resolvedFile.readText().replace(oldValue, newValue))
}

suspend fun HotReloadTestFixture.reloadSourceCode(
    sourceFile: String = getDefaultMainKtSourceFile(),
    source: String,
) {
    writeCode(sourceFile, source)

    logger.quiet("Waiting for reload request")
    val reloadRequest = run {
        val reloadRequest = skipToMessage<ReloadClassesRequest>()
        if (reloadRequest.changedClassFiles.isEmpty()) fail("No changedClassFiles in reload request")
        if (reloadRequest.changedClassFiles.size > 1) fail("Too many changedClassFiles in reload request: ${reloadRequest.changedClassFiles}")
        val (requestedFile, changeType) = reloadRequest.changedClassFiles.entries.single()
        val sourceFileName = projectDir.resolve(sourceFile).nameWithoutExtension
        requestedFile.name.assertMatchesRegex(""".*${sourceFileName}Kt.*\.class""")
        assertEquals(ReloadClassesRequest.ChangeType.Modified, changeType)
        reloadRequest
    }

    logger.quiet("Waiting for UI render")
    run {
        val rendered = skipToMessage<UIRendered>()
        assertEquals(reloadRequest.messageId, rendered.reloadRequestId)
    }
}

suspend fun HotReloadTestFixture.awaitReload() {
    val reloadRequest = skipToMessage<ReloadClassesRequest>()
    val rendered = skipToMessage<UIRendered>()
    assertEquals(reloadRequest.messageId, rendered.reloadRequestId)
}

private fun HotReloadTestFixture.writeCode(
    sourceFile: String = getDefaultMainKtSourceFile(),
    @Language("kotlin") source: String
): Path {
    val resolvedFile = projectDir.resolve(sourceFile)
    resolvedFile.createParentDirectories()
    resolvedFile.writeText(source)
    return resolvedFile
}

internal fun HotReloadTestFixture.getDefaultMainKtSourceFile(): String {
    return when (projectMode) {
        ProjectMode.Kmp -> "src/commonMain/kotlin/Main.kt"
        ProjectMode.Jvm -> "src/main/kotlin/Main.kt"
    }
}

