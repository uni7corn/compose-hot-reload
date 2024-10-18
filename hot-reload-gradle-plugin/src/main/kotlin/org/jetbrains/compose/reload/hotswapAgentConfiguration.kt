package org.jetbrains.compose.reload

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration


private const val hotswapAgentConfigurationName = "composeHotReloadHotswapAgent"

internal val Project.hotswapAgentConfiguration: Configuration
    get() {
        configurations.findByName(hotswapAgentConfigurationName)?.let { return it }
        return configurations.create(hotswapAgentConfigurationName) {configuration ->
            configuration.isCanBeConsumed = false
            configuration.isCanBeResolved = true
            configuration.isTransitive = false
            configuration.dependencies.add(project.dependencies.create(HOTSWAP_AGENT_CORE))
        }
    }