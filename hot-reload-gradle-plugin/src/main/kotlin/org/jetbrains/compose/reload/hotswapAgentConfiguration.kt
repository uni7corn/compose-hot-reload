package org.jetbrains.compose.reload

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration


private const val composeHotReloadAgentConfigurationName = "composeHotReloadHotswapAgent"

internal val Project.composeHotReloadAgentConfiguration: Configuration
    get() {
        configurations.findByName(composeHotReloadAgentConfigurationName)?.let { return it }
        return configurations.create(composeHotReloadAgentConfigurationName) { configuration ->
            configuration.isCanBeConsumed = false
            configuration.isCanBeResolved = true
            configuration.isTransitive = false

            configuration.dependencies.add(
                project.dependencies.create("org.jetbrains.compose:hot-reload-agent:$HOT_RELOAD_VERSION")
            )
        }
    }
