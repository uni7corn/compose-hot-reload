package org.jetbrains.compose.reload

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.compose.reload.orchestration.ORCHESTRATION_SERVER_PORT_PROPERTY_KEY

internal val Project.orchestrationPort: Provider<Int>
    get() = (providers.systemProperty(ORCHESTRATION_SERVER_PORT_PROPERTY_KEY)
        .orElse(providers.gradleProperty(ORCHESTRATION_SERVER_PORT_PROPERTY_KEY)))
        .map { value -> value.toInt() }
