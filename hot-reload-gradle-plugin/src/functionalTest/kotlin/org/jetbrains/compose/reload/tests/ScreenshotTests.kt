@file:Suppress("DuplicatedCode")

package org.jetbrains.compose.reload.tests

import org.jetbrains.compose.reload.utils.*

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
}
