@file:Suppress("DuplicatedCode")

package org.jetbrains.compose.reload.tests

import org.jetbrains.compose.reload.utils.*
import kotlin.io.path.appendLines
import kotlin.io.path.appendText

class ScreenshotTests {

    @ScreenshotTest
    @DefaultBuildGradleKts
    fun `test - simple change`(fixture: ScreenshotTestFixture) = fixture.runTest {
        fixture initialSourceCode """
            import androidx.compose.foundation.layout.*
            import androidx.compose.material3.Text
            import androidx.compose.ui.unit.sp
            import androidx.compose.ui.window.*
            import org.jetbrains.compose.reload.underTest.*
            
            fun main() {
                underTestApplication {
                    Text("Hello", fontSize = 48.sp)
                }
            }
            """.trimIndent()
        fixture.checkScreenshot("before")

        fixture.replaceSourceCodeAndReload("Hello", "Goodbye")
        fixture.checkScreenshot("after")
    }

    @ScreenshotTest
    @DefaultBuildGradleKts
    fun `test - retained state`(fixture: ScreenshotTestFixture) = fixture.runTest {
        val d = "\$"
        fixture initialSourceCode """
            import androidx.compose.foundation.layout.*
            import androidx.compose.material3.*
            import androidx.compose.ui.unit.*
            import androidx.compose.ui.window.*
            import androidx.compose.runtime.*
            import org.jetbrains.compose.reload.underTest.*
            
            fun main() {
                underTestApplication {
                    var state by remember { mutableStateOf(0) }
                    onTestEvent {
                        state++
                    }
                    Text("Before: ${d}state", fontSize = 48.sp)
                }
            }
            """.trimIndent()
        fixture.checkScreenshot("before-0")

        fixture.hotReloadTestFixture.sendTestEvent()
        fixture.checkScreenshot("before-1")

        fixture.hotReloadTestFixture.sendTestEvent()
        fixture.checkScreenshot("before-2")

        fixture.replaceSourceCodeAndReload("Before", "After")
        fixture.checkScreenshot("after-2")
    }

    @AndroidScreenshotTest
    @DefaultAndroidAndJvmBuildSetup
    fun `test - kmp with android and jvm`(fixture: ScreenshotTestFixture) = fixture.runTest {
        fixture initialSourceCode """
            import androidx.compose.foundation.layout.*
            import androidx.compose.material3.Text
            import androidx.compose.ui.unit.sp
            import androidx.compose.ui.window.*
            import org.jetbrains.compose.reload.underTest.*
            
            fun main() {
                underTestApplication {
                    Text("Hello", fontSize = 48.sp)
                }
            }
            """.trimIndent()
        fixture.checkScreenshot("before")

        fixture.replaceSourceCodeAndReload("Hello", "Goodbye")
        fixture.checkScreenshot("after")
    }

    @ScreenshotTest
    @DefaultBuildGradleKts("app", "widgets")
    fun `test - change in dependency project`(fixture: ScreenshotTestFixture) = fixture.runTest {
        fixture.hotReloadTestFixture.projectDir.settingsGradleKts.appendLines(
            listOf(
                "",
                """include(":widgets")""",
                """include(":app")"""
            )
        )

        if (fixture.projectMode == ProjectMode.Kmp) {
            fixture.hotReloadTestFixture.projectDir.subproject("app").buildGradleKts.appendText(
                """
                kotlin {
                    sourceSets.commonMain.dependencies {
                        implementation(project(":widgets"))
                    }
                }
                """.trimIndent()
            )
        }

        if (fixture.projectMode == ProjectMode.Jvm) {
            fixture.hotReloadTestFixture.projectDir.subproject("app").buildGradleKts.appendText(
                """
                dependencies {
                    implementation(project(":widgets"))
                }
            """.trimIndent()
            )
        }

        fixture.hotReloadTestFixture.projectDir.writeText(
            "widgets/src/${fixture.projectMode.fold("commonMain", "main")}/kotlin/Widget.kt", """
            import androidx.compose.material3.Text
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.unit.sp
            
            @Composable
            fun Widget(text: String) {
                Text("Before: " + text, fontSize = 48.sp)
            }
            """.trimIndent()
        )

        fixture.hotReloadTestFixture.projectDir.writeText(
            "app/src/${fixture.projectMode.fold("commonMain", "main")}/kotlin/Main.kt", """
            import androidx.compose.foundation.layout.*
            import androidx.compose.material3.Text
            import androidx.compose.ui.unit.sp
            import androidx.compose.ui.window.*
            import org.jetbrains.compose.reload.underTest.*
            
            fun main() {
                underTestApplication {
                    Widget("Hello") // <- calls into other module
                }
            }
            """.trimIndent()
        )

        fixture.launchApplicationAndWait(":app")
        fixture.checkScreenshot("before")

        fixture.replaceSourceCodeAndReload(
            "widgets/src/${fixture.projectMode.fold("commonMain", "main")}/kotlin/Widget.kt",
            "Before:", "After:"
        )
        fixture.checkScreenshot("after")
    }
}
