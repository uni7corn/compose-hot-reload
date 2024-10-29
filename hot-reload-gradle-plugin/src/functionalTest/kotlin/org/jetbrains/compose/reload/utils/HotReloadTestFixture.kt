@file:OptIn(ExperimentalPathApi::class)

package org.jetbrains.compose.reload.utils

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.OrchestrationServer
import org.jetbrains.compose.reload.orchestration.asChannel
import java.io.Serializable
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class HotReloadTestFixture(
    val testClassName: String,
    val testMethodName: String,
    val projectDir: ProjectDir,
    val gradleRunner: GradleRunner,
    val orchestration: OrchestrationServer,
    val projectMode: ProjectMode
) : AutoCloseable {

    val logger: Logger = Logging.getLogger("ScreenshotTestFixture")

    val messages = orchestration.asChannel()

    fun sendMessage(message: OrchestrationMessage) {
        orchestration.sendMessage(message).get()
    }

    suspend inline fun <reified T> skipToMessage(timeout: Duration = 1.minutes): T {
        logger.quiet("Waiting for message ${T::class.simpleName}")

        return withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(timeout) {
                messages.receiveAsFlow().filterIsInstance<T>().first()
            }
        }
    }

    lateinit var testScope: TestScope
        private set

    /**
     * Coroutines launched in this scope will not keep the 'runTest' blocking.
     * This scope will be canceled after the 'runTest' finished (e.g., useful for launching 'Daemon Coroutines)
     */
    lateinit var daemonTestScope: CoroutineScope

    fun runTest(timeout: Duration = 1.minutes, test: suspend () -> Unit) {
        kotlinx.coroutines.test.runTest(timeout = timeout) {
            testScope = this
            daemonTestScope = CoroutineScope(currentCoroutineContext() + Job(currentCoroutineContext().job))

            daemonTestScope.launch {
                orchestration.asChannel().consumeAsFlow().collect {
                    logger.quiet("Test: Received message: $it")
                }
            }

            try {
                test()
            } finally {
                daemonTestScope.cancel()
            }
        }
    }

    private val resourcesLock = ReentrantLock()
    private val resources = mutableListOf<AutoCloseable>()

    override fun close() {
        orchestration.close()
        projectDir.path.deleteRecursively()

        testScope.cancel()
        daemonTestScope.cancel()

        resourcesLock.withLock {
            resources.forEach { resource -> resource.close() }
            resources.clear()
        }
    }
}

fun HotReloadTestFixture.sendTestEvent(payload: Serializable? = null) {
    sendMessage(OrchestrationMessage.TestEvent(payload))
}

suspend fun HotReloadTestFixture.launchDaemonThread(block: () -> Unit): Job {
    val threadResult = CompletableDeferred<Unit>()
    val thread = thread(isDaemon = true) {
        try {
            block()
            threadResult.complete(Unit)
        } catch (_: InterruptedException) {
            threadResult.complete(Unit)
            // Goodbye.
        } catch (t: Throwable) {
            threadResult.completeExceptionally(t)
        }
    }

    currentCoroutineContext().job.invokeOnCompletion {
        thread.interrupt()
    }

    /* We want to forward exceptions from the thread into the parent coroutine scope */
    return daemonTestScope.launch {
        threadResult.await()
    }
}

suspend fun HotReloadTestFixture.launchApplication(
    projectPath: String = ":",
    mainClass: String = "MainKt"
) {
    launchDaemonThread {
        val runTask = when (projectMode) {
            ProjectMode.Kmp -> "jvmRun"
            ProjectMode.Jvm -> "run"
        }

        val additionalArguments = when (projectMode) {
            ProjectMode.Kmp -> arrayOf("-DmainClass=$mainClass")
            ProjectMode.Jvm -> arrayOf()
        }

        val runTaskPath = buildString {
            if (projectPath != ":") {
                append(projectPath)
            }

            append(":")
            append(runTask)
        }

        gradleRunner
            .addedArguments("wrapper", runTaskPath, *additionalArguments)
            .build()
    }
}
