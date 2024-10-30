package org.jetbrains.compose.reload.tests

import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.utils.*
import kotlin.test.assertEquals

class ErrorRecoveryTests {

    @HotReloadTest
    @TestOnlyLatestVersions
    @DefaultBuildGradleKts
    @DefaultSettingsGradleKts
    fun `good - bad - good`(fixture: HotReloadTestFixture) = fixture.runTest {
        val code = fixture.initialSourceCode(
            """
            import androidx.compose.material3.Text
            import org.jetbrains.compose.reload.underTest.*
            
            fun main() {
                underTestApplication {
                    Text("Hello")
                }
            }
        """.trimIndent()
        )

        fixture.checkScreenshot("0-initial")
        code.replaceText("""Text("Hello")""", """error("Foo")""")
        assertEquals("Foo", fixture.skipToMessage<OrchestrationMessage.UIException>().message)

        code.replaceText("""error("Foo")""", """Text("Recovered")""")
        fixture.awaitReload()
        fixture.checkScreenshot("1-recovered")
    }
}