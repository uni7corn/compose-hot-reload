package org.jetbrains.compose.reload.utils

import org.jetbrains.compose.reload.HOT_RELOAD_VERSION
import java.nio.file.Path
import kotlin.io.path.writeText

enum class ProjectMode {
    Kmp, Jvm
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

