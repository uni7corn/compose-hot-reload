/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.compose.reload.tests.gradle

import kotlinx.coroutines.flow.toList
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ShutdownRequest
import org.jetbrains.compose.reload.test.gradle.ApplicationLaunchMode
import org.jetbrains.compose.reload.test.gradle.BuildMode
import org.jetbrains.compose.reload.test.gradle.HotReloadTest
import org.jetbrains.compose.reload.test.gradle.HotReloadTestFixture
import org.jetbrains.compose.reload.test.gradle.ProjectMode
import org.jetbrains.compose.reload.test.gradle.TestedBuildMode
import org.jetbrains.compose.reload.test.gradle.TestedLaunchMode
import org.jetbrains.compose.reload.test.gradle.TestedProjectMode
import org.jetbrains.compose.reload.test.gradle.assertSuccessful
import org.jetbrains.compose.reload.test.gradle.buildFlow
import org.jetbrains.compose.reload.test.gradle.checkScreenshot
import org.jetbrains.compose.reload.test.gradle.getDefaultMainKtSourceFile
import org.jetbrains.compose.reload.utils.GradleIntegrationTest
import org.jetbrains.compose.reload.utils.QuickTest
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

class HijackRunTest {

    @GradleIntegrationTest
    @HotReloadTest
    @TestedLaunchMode(ApplicationLaunchMode.GradleBlocking)
    @TestedBuildMode(BuildMode.Continuous)
    @TestedProjectMode(ProjectMode.Kmp)
    @QuickTest
    fun `test - run with jvmRun`(fixture: HotReloadTestFixture) = fixture.runTest {
        projectDir.resolve(getDefaultMainKtSourceFile()).createParentDirectories().writeText(
            """
            import org.jetbrains.compose.reload.test.*
            fun main() {
                screenshotTestApplication {
                    TestText("1")
                }
            }
        """.trimIndent()
        )


        val jvmRun = runTransaction {
            val jvmRun = fixture.launchTestDaemon {
                fixture.gradleRunner.buildFlow("jvmRun", "-PmainClass=MainKt").toList().assertSuccessful()
            }

            awaitApplicationStart()
            jvmRun
        }
        checkScreenshot("1")

        /* We expect that the jvmRun uses auto recompile */
        runTransaction {
            replaceSourceCode("1", "2")
            awaitReload()
        }
        checkScreenshot("2")

        sendMessage(ShutdownRequest("Requested by test")) {
            jvmRun.await()
        }
    }
}
