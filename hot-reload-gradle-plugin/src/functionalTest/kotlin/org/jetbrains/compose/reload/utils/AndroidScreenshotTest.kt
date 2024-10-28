package org.jetbrains.compose.reload.utils

import org.jetbrains.compose.reload.utils.ProjectMode.Kmp
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import org.junit.platform.commons.util.AnnotationUtils
import java.util.stream.Stream
import kotlin.streams.asStream

@TestTemplate
@ExtendWith(AndroidScreenshotTestInvocationContextProvider::class)
@DefaultSettingsGradleKts
annotation class AndroidScreenshotTest

class AndroidScreenshotTestInvocationContextProvider : TestTemplateInvocationContextProvider {
    private val hotReloadInvocationContextProvider = HotReloadTestInvocationContextProvider()

    override fun supportsTestTemplate(context: ExtensionContext): Boolean {
        return hotReloadInvocationContextProvider.supportsTestTemplate(context) &&
                AnnotationUtils.findAnnotation(context.testMethod, AndroidScreenshotTest::class.java).isPresent
    }

    override fun provideTestTemplateInvocationContexts(
        context: ExtensionContext
    ): Stream<TestTemplateInvocationContext> {
        val testedVersions = TestedVersions(
            gradle = TestedGradleVersion.entries.last(),
            kotlin = TestedKotlinVersion.entries.last(),
            compose = TestedComposeVersion.entries.last()
        )

        return TestedAndroidVersion.entries.map { androidVersion ->
            val hotReloadTestInvocationContext = HotReloadTestInvocationContext(testedVersions)
            val screenshotTestInvocationContext = ScreenshotTestInvocationContext(hotReloadTestInvocationContext, Kmp)
            AndroidScreenshotTestInvocationContext(screenshotTestInvocationContext, androidVersion)
        }.asSequence().asStream()
    }

}

class AndroidScreenshotTestInvocationContext(
    private val parent: TestTemplateInvocationContext,
    private val androidVersion: TestedAndroidVersion,
) : TestTemplateInvocationContext {
    override fun getDisplayName(invocationIndex: Int): String? {
        return "Android $androidVersion " + parent.getDisplayName(invocationIndex)
    }

    override fun getAdditionalExtensions(): List<Extension> {
        return parent.additionalExtensions + listOf(
            object : BeforeEachCallback {
                override fun beforeEach(context: ExtensionContext) {
                    context.androidVersion = androidVersion
                }
            }
        )
    }
}