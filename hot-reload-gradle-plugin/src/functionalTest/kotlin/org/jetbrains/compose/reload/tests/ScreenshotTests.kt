package org.jetbrains.compose.reload.tests

import org.jetbrains.compose.reload.utils.*

class ScreenshotTests {

    @ScreenshotTest
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
}

