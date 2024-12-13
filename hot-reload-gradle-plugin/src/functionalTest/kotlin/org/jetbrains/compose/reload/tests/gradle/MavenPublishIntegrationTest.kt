package org.jetbrains.compose.reload.tests.gradle

import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.compose.reload.utils.addedArguments
import org.jetbrains.compose.reload.utils.createDefaultSettingsGradleKtsContent
import org.jetbrains.compose.reload.utils.withProjectTestKitDir
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

class MavenPublishIntegrationTest {
    @Test
    fun `test - publish with compose hot reload plugin applied - jvm`(@TempDir dir: Path) {
        dir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                kotlin("jvm")
                kotlin("plugin.compose")
                id("org.jetbrains.compose")
                id("org.jetbrains.compose-hot-reload")
                `maven-publish`
            }
            
            group = "org.tested"
            
            publishing {
                publications.create("maven", MavenPublication::class) {
                    from(components["java"])
                }
            }
            
            publishing {
                repositories {
                    maven(file("build/repo")) {
                        name = "local"
                    }
                }
            }
            
            dependencies {
                implementation(compose.desktop.currentOs)
            }
            
            """.trimIndent()
        )

        dir.resolve("settings.gradle.kts").writeText(createDefaultSettingsGradleKtsContent())
        dir.resolve("src/main/kotlin/Main.kt").createParentDirectories().writeText("fun main() {}")

        GradleRunner.create()
            .withProjectDir(dir.toFile())
            .withProjectTestKitDir()
            .addedArguments(":publishAllPublicationsToLocalRepository")
            .build()
    }

    @Test
    fun `test - publish with compose hot reload plugin applied - kmp`(@TempDir dir: Path) {
        dir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                kotlin("multiplatform")
                kotlin("plugin.compose")
                id("org.jetbrains.compose")
                id("org.jetbrains.compose-hot-reload")
                `maven-publish`
            }
            
            group = "org.tested"
            
            publishing {
                repositories {
                    maven(file("build/repo")) {
                        name = "local"
                    }
                }
            }
            
            kotlin {
                jvm()
                sourceSets.commonMain.dependencies {
                    implementation(compose.desktop.currentOs)
                }
            }
            """.trimIndent()
        )

        dir.resolve("settings.gradle.kts").writeText(createDefaultSettingsGradleKtsContent())
        dir.resolve("src/main/kotlin/Main.kt").createParentDirectories().writeText("fun main() {}")

        GradleRunner.create()
            .withProjectDir(dir.toFile())
            .withProjectTestKitDir()
            .addedArguments(":publishAllPublicationsToLocalRepository")
            .build()
    }
}
