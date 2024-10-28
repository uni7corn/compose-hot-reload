package org.jetbrains.compose.reload.utils

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.support.AnnotationSupport


annotation class DefaultBuildGradleKts(val mode: ProjectMode = ProjectMode.Jvm)

internal class DefaultBuildGradleKtsExtension(
) : BeforeTestExecutionCallback {

    override fun beforeTestExecution(context: ExtensionContext) {
        val annotation = AnnotationSupport.findAnnotation(context.requiredTestMethod, DefaultBuildGradleKts::class.java)
        if (annotation.isEmpty) return

        val testFixture = context.getHotReloadTestFixtureOrThrow()
        when (annotation.get().mode) {
            ProjectMode.Kmp -> testFixture.setupKmpProject()
            ProjectMode.Jvm -> testFixture.setupJvmProject()
        }
    }
}