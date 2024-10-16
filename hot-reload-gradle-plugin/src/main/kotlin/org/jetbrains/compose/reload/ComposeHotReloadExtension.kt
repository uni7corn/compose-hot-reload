package org.jetbrains.compose.reload

import org.gradle.api.Project

open class ComposeHotReloadExtension(internal val project: Project) {
    val useJetBrainsRuntime = project.objects.property(Boolean::class.java).convention(false)
}