@file:Suppress("DuplicatedCode", "FunctionName")

package org.jetbrains.compose.reload.tests

import org.jetbrains.compose.reload.utils.*
import kotlin.io.path.appendLines
import kotlin.io.path.appendText

class ScreenshotTests {

    @HotReloadTest
    @DefaultSettingsGradleKts
    @DefaultBuildGradleKts
    fun `test - simple change`(fixture: HotReloadTestFixture) = fixture.runTest {
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

    @HotReloadTest
    @DefaultSettingsGradleKts
    @DefaultBuildGradleKts
    fun `test - retained state`(fixture: HotReloadTestFixture) = fixture.runTest {
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

        fixture.sendTestEvent()
        fixture.checkScreenshot("before-1")

        fixture.sendTestEvent()
        fixture.checkScreenshot("before-2")

        fixture.replaceSourceCodeAndReload("Before", "After")
        fixture.checkScreenshot("after-2")
    }

    @AndroidHotReloadTest
    @DefaultSettingsGradleKts
    @DefaultAndroidAndJvmBuildSetup
    fun `test - kmp with android and jvm`(fixture: HotReloadTestFixture) = fixture.runTest {
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

    @HotReloadTest
    @DefaultSettingsGradleKts
    @DefaultBuildGradleKts("app", "widgets")
    fun `test - change in dependency project`(fixture: HotReloadTestFixture) = fixture.runTest {
        fixture.projectDir.settingsGradleKts.appendLines(
            listOf(
                "",
                """include(":widgets")""",
                """include(":app")"""
            )
        )

        if (fixture.projectMode == ProjectMode.Kmp) {
            fixture.projectDir.subproject("app").buildGradleKts.appendText(
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
            fixture.projectDir.subproject("app").buildGradleKts.appendText(
                """
                dependencies {
                    implementation(project(":widgets"))
                }
            """.trimIndent()
            )
        }

        fixture.projectDir.writeText(
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

        fixture.projectDir.writeText(
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

    @HotReloadTest
    @DefaultSettingsGradleKts
    @DefaultBuildGradleKts
    @TestOnlyLatestVersions
    @MinKotlinVersion("2.1.20-dev-2637")
    fun `test - add button`(fixture: HotReloadTestFixture) = fixture.runTest {
        fixture initialSourceCode """
            import androidx.compose.foundation.layout.*
            import androidx.compose.material3.*
            import androidx.compose.ui.unit.*
            import androidx.compose.ui.window.*
            import androidx.compose.runtime.*
            import org.jetbrains.compose.reload.underTest.*
            
            fun main() {
                underTestApplication {
                    Column {
                        Text("Initial", fontSize = 48.sp)
                        // Add button
                    }
                }
            }
            """.trimIndent()
        fixture.checkScreenshot("0-before")

        fixture.replaceSourceCodeAndReload(
            """// Add button""", """
                Button(onClick = { }) {
                    Text("Button")
                }
            """.trimIndent()
        )
        fixture.checkScreenshot("1-withButton")
    }


    @HotReloadTest
    @DefaultSettingsGradleKts
    @DefaultBuildGradleKts
    @TestOnlyLatestVersions
    @MinKotlinVersion("2.1.20-dev-2637")
    fun `test - add remembered state`(fixture: HotReloadTestFixture) = fixture.runTest {
        fixture initialSourceCode """
            import androidx.compose.foundation.layout.*
            import androidx.compose.material3.*
            import androidx.compose.ui.unit.*
            import androidx.compose.ui.window.*
            import androidx.compose.runtime.*
            import org.jetbrains.compose.reload.underTest.*
            
            fun main() {
                underTestApplication {
                    // add state
                    
                    // add effect
                    
                    Column {
                        Text("Initial", fontSize = 48.sp)
                        // Add button
                    }
                }
            }
            """.trimIndent()
        fixture.checkScreenshot("0-before")

        fixture.replaceSourceCodeAndReload("""// add state""", """var state by remember { mutableStateOf(0) }""")
        fixture.checkScreenshot("1-addedState")

        fixture.replaceSourceCodeAndReload(""""Initial"""", """"State: %state"""".replace("%", "$"))
        fixture.checkScreenshot("2-renderState")

        fixture.replaceSourceCodeAndReload("""// add effect""", """onTestEvent { state++ }""")
        fixture.checkScreenshot("3-addedEffect")

        fixture.sendTestEvent()
        fixture.checkScreenshot("4-afterEvent")
    }
}
