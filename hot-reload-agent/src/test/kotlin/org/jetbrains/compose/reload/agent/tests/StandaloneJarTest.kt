/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.compose.reload.agent.tests

import org.jetbrains.compose.reload.agent.orchestration
import org.jetbrains.compose.reload.core.HotReloadProperty
import org.jetbrains.compose.reload.core.testFixtures.sanitized
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.asBlockingQueue
import org.jetbrains.compose.reload.orchestration.startOrchestrationServer
import org.jetbrains.compose.reload.test.core.TestEnvironment
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createParentDirectories
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writeText
import kotlin.system.exitProcess
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.fail

class StandaloneJarTest {
    val standaloneJar = Path(
        System.getProperty("agent-standalone.jar") ?: error("agent-standalone.jar system property is not set")
    )

    private val cleanupActions = mutableListOf<() -> Unit>()

    @AfterTest
    fun cleanup() {
        cleanupActions.forEach { runCatching { it() } }
    }

    @Test
    fun `test - launching process with standalone agent jar`() {
        val server = startOrchestrationServer()
        val messages = server.asBlockingQueue()
        cleanupActions.add { server.close() }

        val currentProcessInfo = ProcessHandle.current().info()
        val testProcess = ProcessBuilder(
            currentProcessInfo.command().get(),
            "-javaagent:${standaloneJar.absolutePathString()}",
            "-cp",
            listOf(
                // Test classes
                StandaloneJarTestMain::class.java.protectionDomain.codeSource.location.file,
                // Kotlin Stdlib
                ArrayDeque::class.java.protectionDomain.codeSource.location.file,
                // Slf4j
                LoggerFactory::class.java.protectionDomain.codeSource.location.file,
            ).joinToString(File.pathSeparator),
            "-D${HotReloadProperty.OrchestrationPort.key}=${server.port}",
            "-D${HotReloadProperty.IsHeadless.key}=true",
            StandaloneJarTestMain::class.qualifiedName,
        ).inheritIO().start()


        cleanupActions.add { testProcess.destroy() }

        if (!testProcess.waitFor(1, TimeUnit.MINUTES)) {
            fail("Test process did not finish in time (hanging?)")
        }

        if (testProcess.exitValue() != 0) {
            fail("Test process exited with code ${testProcess.exitValue()}")
        }

        val receivedMessages = mutableListOf<OrchestrationMessage>()
        messages.drainTo(receivedMessages)

        if (receivedMessages.none { it is OrchestrationMessage.TestEvent && it.payload == "Alive" }) {
            fail("Test process did not send 'Alive' message\nReceived messages: ${receivedMessages.joinToString("\n ")}")
        }
    }

    @Test
    fun `test - standalone agent jar content`() {
        val entries = mutableListOf<String>()
        ZipFile(standaloneJar.toFile()).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                entries.add(entry.name)
            }
        }

        val renderedEntries = entries
            .filter { !it.endsWith(".class") }
            .filter { Path(it).nameCount < 7 } // only 7 levels deep
            .sorted()

        val actualText = renderedEntries.joinToString("\n").sanitized()
        val expectFile = Path("src/test/resources/standalone-jar/content.txt")

        if (TestEnvironment.updateTestData) {
            expectFile.createParentDirectories().writeText(actualText)
            return
        }

        if (!expectFile.toFile().exists()) {
            expectFile.createParentDirectories().writeText(actualText)
            fail("'${expectFile.toUri()}' did not exist; Generated")
        }

        val expectedText = expectFile.toFile().readText().sanitized()
        if (actualText != expectedText) {
            val actualFile = expectFile.resolveSibling("${expectFile.nameWithoutExtension}-actual.${expectFile.extension}")
            actualFile.createParentDirectories().writeText(actualText)
            fail("'${expectFile.toUri()}'  did not match\nGenerated to ${actualFile.toUri()}")
        }
    }
}


internal object StandaloneJarTestMain {
    @JvmStatic
    fun main(args: Array<String>) {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            exitProcess(42)
        }

        orchestration.sendMessage(OrchestrationMessage.TestEvent("Alive")).get()
        exitProcess(0)
    }
}
