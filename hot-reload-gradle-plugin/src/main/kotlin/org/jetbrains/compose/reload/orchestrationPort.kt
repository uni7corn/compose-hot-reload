package org.jetbrains.compose.reload

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.compose.reload.core.HotReloadProperty

internal val Project.orchestrationPort: Provider<Int>
    get() = (providers.systemProperty(HotReloadProperty.OrchestrationPort.key)
        .orElse(providers.gradleProperty(HotReloadProperty.OrchestrationPort.key)))
        .map { value -> value.toInt() }
