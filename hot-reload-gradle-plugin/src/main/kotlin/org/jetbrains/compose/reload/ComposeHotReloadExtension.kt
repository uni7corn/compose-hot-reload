package org.jetbrains.compose.reload

import org.gradle.api.Project

internal const val composeHotReloadExtensionName = "composeHotReload"

internal val Project.composeHotReloadExtension: ComposeHotReloadExtension
    get() = extensions.getByType(ComposeHotReloadExtension::class.java)

open class ComposeHotReloadExtension(internal val project: Project) {
    /**
     * Using the JetBrains Runtime to hot-reload your application has significant advantages
     * It allows for
     * - adding methods
     * - removing methods
     * - adding classes
     * - removing classes
     * - migrates state across changed classes
     *
     * Note: When this flag is switched on, a JetBrains runtime is required locally!
     * Download: https://github.com/JetBrains/JetBrainsRuntime (or use IntelliJ to download JBR 21)
     *
     */
    val useJetBrainsRuntime = project.objects.property(Boolean::class.java).convention(true)
}
