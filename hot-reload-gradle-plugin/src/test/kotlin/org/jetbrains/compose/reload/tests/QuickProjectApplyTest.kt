package org.jetbrains.compose.reload.tests

import org.gradle.kotlin.dsl.findByType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.compose.reload.ComposeHotReloadExtension
import org.jetbrains.compose.reload.ComposeHotReloadPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import kotlin.test.Test
import kotlin.test.assertNotNull

class QuickProjectApplyTest {
    @Test
    fun `test - apply to Kotlin multiplatform`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(KotlinMultiplatformPluginWrapper::class.java)
        project.plugins.apply(ComposeHotReloadPlugin::class.java)

        assertNotNull(project.extensions.findByType<ComposeHotReloadExtension>(), "Expected hot reload extension")
    }

    @Test
    fun `test - apply to Kotlin JVM`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(KotlinPluginWrapper::class.java)
        project.plugins.apply(ComposeHotReloadPlugin::class.java)

        assertNotNull(project.extensions.findByType<ComposeHotReloadExtension>(), "Expected hot reload extension")
    }
}