/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.compose.reload.tests

import org.jetbrains.compose.reload.orchestration.OrchestrationClientRole
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ClientDisconnected
import org.jetbrains.compose.reload.test.gradle.HotReloadTest
import org.jetbrains.compose.reload.test.gradle.HotReloadTestFixture
import org.jetbrains.compose.reload.test.gradle.launchApplication
import org.jetbrains.compose.reload.test.gradle.writeText
import org.jetbrains.compose.reload.utils.QuickTest
import kotlin.time.Duration.Companion.minutes

class ClientDisconnectedMessageTest {

    @HotReloadTest
    @QuickTest
    fun `send shutdown request - receive ClientDisconnected`(
        testFixture: HotReloadTestFixture
    ) = testFixture.runTest(timeout = 5.minutes) {
        testFixture.projectDir.writeText(
            "src/jvmMain/kotlin/Main.kt", """
            import androidx.compose.material3.*
            import org.jetbrains.compose.reload.test.*
            import androidx.compose.ui.unit.sp
            fun main() {
                screenshotTestApplication {
                    TestText("Hello", fontSize = 48.sp)
                }
            }
        """.trimIndent()
        )

        testFixture.runTransaction {
            testFixture.launchApplication()
            skipToMessage<OrchestrationMessage.UIRendered>()
        }

        testFixture.sendMessage(OrchestrationMessage.ShutdownRequest("Explicitly requested by the test")) {
            skipToMessage<ClientDisconnected> { message ->
                message.clientRole == OrchestrationClientRole.Application
            }
        }
    }
}
