/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.compose.reload.tests

import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.compose.reload.AbstractComposeHotRun
import org.jetbrains.compose.reload.gradle.kotlinMultiplatformOrNull
import org.jetbrains.compose.reload.utils.evaluate
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeHotRunTasksTest {
    @Test
    fun `test - default run task name - jvm`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.kotlin.jvm")
        project.plugins.apply("org.jetbrains.compose.hot-reload")
        project.evaluate()

        assertEquals(
            setOf("runHot", "runDev"), project.tasks.withType<AbstractComposeHotRun>().map { it.name }.toSet()
        )
    }

    @Test
    fun `test - default run task name - kmp`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.kotlin.multiplatform")
        project.plugins.apply("org.jetbrains.compose.hot-reload")

        project.kotlinMultiplatformOrNull?.jvm()

        project.evaluate()

        assertEquals(
            setOf("jvmRunHot", "jvmRunDev"), project.tasks.withType<AbstractComposeHotRun>().map { it.name }.toSet()
        )
    }
}
