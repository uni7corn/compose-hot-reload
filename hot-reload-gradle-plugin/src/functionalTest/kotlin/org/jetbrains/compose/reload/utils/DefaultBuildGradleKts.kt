package org.jetbrains.compose.reload.utils

import org.jetbrains.compose.reload.HOT_RELOAD_VERSION
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.support.AnnotationSupport
import kotlin.io.path.writeText

@ExtendWith(DefaultBuildGradleKtsExtension::class)
annotation class DefaultBuildGradleKts

private class DefaultBuildGradleKtsExtension() : BeforeTestExecutionCallback {

    override fun beforeTestExecution(context: ExtensionContext) {
        val annotation = AnnotationSupport.findAnnotation(context.requiredTestMethod, DefaultBuildGradleKts::class.java)
        if (annotation.isEmpty) return

        val testFixture = context.getHotReloadTestFixtureOrThrow()
        when (context.projectMode) {
            ProjectMode.Kmp -> testFixture.setupKmpProject()
            ProjectMode.Jvm -> testFixture.setupJvmProject()
            null -> return
        }
    }
}

fun HotReloadTestFixture.setupKmpProject(
    targets: List<String> = listOf("jvm")
) {
    projectDir.buildGradleKts.writeText(
        """
        plugins {
            kotlin("multiplatform")
            kotlin("plugin.compose")
            id("org.jetbrains.compose")
            id("org.jetbrains.compose-hot-reload")
        }
        
        kotlin {
            ${targets.joinToString("\n") { "$it()" }}
            
            sourceSets.commonMain.dependencies {
                implementation(compose.foundation)
                implementation(compose.material3)
            }
            
            sourceSets.jvmMain.dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.compose:hot-reload-under-test:$HOT_RELOAD_VERSION")
            }
        }
       
    """.trimIndent()
    )
}

fun HotReloadTestFixture.setupJvmProject() {
    projectDir.buildGradleKts.writeText(
        """
        import org.jetbrains.compose.reload.ComposeHotRun
        
        plugins {
            kotlin("jvm")
            kotlin("plugin.compose")
            id("org.jetbrains.compose")
            id("org.jetbrains.compose-hot-reload")
        }
        
        dependencies {
            implementation(compose.desktop.currentOs)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation("org.jetbrains.compose:hot-reload-under-test:$HOT_RELOAD_VERSION")
        }
        tasks.create<ComposeHotRun>("run") {
            mainClass.set("MainKt")
        }
    """.trimIndent()
    )
}

