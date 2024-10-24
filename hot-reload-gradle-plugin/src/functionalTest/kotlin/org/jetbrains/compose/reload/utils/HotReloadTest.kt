@file:OptIn(ExperimentalPathApi::class)

package org.jetbrains.compose.reload.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.compose.reload.orchestration.ORCHESTRATION_SERVER_PORT_PROPERTY_KEY
import org.jetbrains.compose.reload.orchestration.OrchestrationServer
import org.jetbrains.compose.reload.orchestration.asChannel
import org.jetbrains.compose.reload.orchestration.startOrchestrationServer
import org.jetbrains.compose.reload.utils.HotReloadTestFixtureProvider.Companion.testFixtureKey
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.*
import java.nio.file.Files
import java.util.concurrent.locks.ReentrantLock
import java.util.stream.Stream
import kotlin.concurrent.withLock
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@TestTemplate
@ExtendWith(HotReloadTestInvocationContextProvider::class)
annotation class HotReloadTest

class HotReloadTestFixture(
    val projectDir: ProjectDir,
    val gradleRunner: GradleRunner,
    val orchestration: OrchestrationServer
) : AutoCloseable {

    val messages = orchestration.asChannel()

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

internal class HotReloadTestInvocationContextProvider : TestTemplateInvocationContextProvider {
    override fun supportsTestTemplate(context: ExtensionContext): Boolean {
        if (context.testMethod.isEmpty) return false
        return context.testMethod.get().isAnnotationPresent(HotReloadTest::class.java)
    }

    override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
        val testedVersions = TestedGradleVersion.entries.flatMap { testedGradleVersion ->
            TestedKotlinVersion.entries.flatMap { testedKotlinVersion ->
                TestedComposeVersion.entries.map { testedComposeVersion ->
                    TestedVersions(
                        gradle = testedGradleVersion,
                        kotlin = testedKotlinVersion,
                        compose = testedComposeVersion,
                    )
                }
            }
        }

        return testedVersions.stream().map { versions -> HotReloadTestInvocationContext(versions) }
    }
}

class HotReloadTestInvocationContext(
    private val versions: TestedVersions,
) : TestTemplateInvocationContext {
    override fun getDisplayName(invocationIndex: Int): String {
        return "Gradle ${versions.gradle.version}, Kotlin ${versions.kotlin.version}, Compose ${versions.compose.version}"
    }

    override fun getAdditionalExtensions(): List<Extension> {
        return listOf(
            SimpleValueProvider(versions),
            SimpleValueProvider(versions.gradle),
            SimpleValueProvider(versions.kotlin),
            SimpleValueProvider(versions.compose),
            HotReloadTestFixtureProvider(versions),
            DefaultSettingsGradleKtsExtension(versions)
        )
    }
}

private inline fun <reified T : Any> SimpleValueProvider(value: T): SimpleValueProvider<T> {
    return SimpleValueProvider(T::class.java, value)
}

private class SimpleValueProvider<T : Any>(
    private val type: Class<T>, private val value: T,
) : ParameterResolver {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == type
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return value
    }
}

private class HotReloadTestFixtureProvider(private val versions: TestedVersions) :
    ParameterResolver, BeforeEachCallback, AfterEachCallback {

    companion object {
        const val testFixtureKey = "hotReloadTestFixture"
    }

    override fun beforeEach(context: ExtensionContext) {
        context.getOrCreateTestFixture()
    }

    override fun afterEach(context: ExtensionContext) {
        context.getHotReloadTestFixtureOrThrow().close()
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type in listOf(
            HotReloadTestFixture::class.java,
            GradleRunner::class.java, ProjectDir::class.java
        )
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? {
        return when (parameterContext.parameter.type) {
            HotReloadTestFixture::class.java -> extensionContext.getHotReloadTestFixtureOrThrow()
            else -> throw IllegalArgumentException("Unknown type: ${parameterContext.parameter.type}")
        }
    }

    private fun ExtensionContext.getOrCreateTestFixture(): HotReloadTestFixture {
        return getStore(namespace).getOrComputeIfAbsent(
            testFixtureKey,
            { createTestFixture() },
            HotReloadTestFixture::class.java
        )
    }

    private fun createTestFixture(): HotReloadTestFixture {
        val projectDir = ProjectDir(Files.createTempDirectory("hot-reload-test"))
        val orchestrationServer = startOrchestrationServer()
        val gradleRunner = GradleRunner.create()
            .withProjectDir(projectDir.path.toFile())
            .withGradleVersion(versions.gradle.version.version)
            .forwardOutput()
            .addedArguments("-PcomposeVersion=${versions.compose}")
            .addedArguments("-PkotlinVersion=${versions.kotlin}")
            .addedArguments("-P$ORCHESTRATION_SERVER_PORT_PROPERTY_KEY=${orchestrationServer.port}")
            .addedArguments("-D$ORCHESTRATION_SERVER_PORT_PROPERTY_KEY=${orchestrationServer.port}")
            .addedArguments("--configuration-cache")
            //.addedArguments("-Pcompose.reload.debug=true")
            .addedArguments("-i")
            .addedArguments("-s")

        return HotReloadTestFixture(projectDir, gradleRunner, orchestrationServer)
    }
}

internal fun ExtensionContext.getHotReloadTestFixtureOrThrow(): HotReloadTestFixture {
    return getStore(namespace).get(testFixtureKey, HotReloadTestFixture::class.java)
        ?: error("Missing '${HotReloadTestFixture::class.simpleName}'")
}