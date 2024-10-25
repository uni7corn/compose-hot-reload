@file:OptIn(ExperimentalPathApi::class)

package org.jetbrains.compose.reload.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.OrchestrationServer
import org.jetbrains.compose.reload.orchestration.asChannel
import java.io.Serializable
import java.util.concurrent.locks.ReentrantLock
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
    val orchestration: OrchestrationServer
) : AutoCloseable {

    val messages = orchestration.asChannel()

    fun sendMessage(message: OrchestrationMessage) {
        orchestration.sendMessage(message).get()
    }

    suspend inline fun <reified T> skipToMessage(timeout: Duration = 1.minutes): T {
        return withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(timeout) {
                messages.receiveAsFlow().filterIsInstance<T>().first()
            }
        }
    }

    private val resourcesLock = ReentrantLock()
    private val resources = mutableListOf<AutoCloseable>()

    override fun close() {
        orchestration.close()
        projectDir.path.deleteRecursively()

        resourcesLock.withLock {
            resources.forEach { resource -> resource.close() }
            resources.clear()
        }
    }
}

suspend fun HotReloadTestFixture.sendTestEvent(payload: Serializable? = null) {
    sendMessage(OrchestrationMessage.TestEvent(payload))
}