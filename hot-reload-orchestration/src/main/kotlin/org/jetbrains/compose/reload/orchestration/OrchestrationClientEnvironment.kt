package org.jetbrains.compose.reload.orchestration


public const val ORCHESTRATION_SERVER_PORT_PROPERTY_KEY: String = "compose.reload.orchestration.port"

internal object OrchestrationClientEnvironment {

    val orchestrationServerPort: Int? =
        System.getenv("COMPOSE_RELOAD_ORCHESTRATION_PORT")?.toIntOrNull()
            ?: System.getProperty(ORCHESTRATION_SERVER_PORT_PROPERTY_KEY)?.toIntOrNull()
}

public fun OrchestrationClient(role: OrchestrationClientRole): OrchestrationClient? {
    if (OrchestrationClientEnvironment.orchestrationServerPort == null) return null

    return connectOrchestrationClient(
        role, port = OrchestrationClientEnvironment.orchestrationServerPort
    )
}