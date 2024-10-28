package org.jetbrains.compose.reload.orchestration

import java.io.Serializable
import java.util.UUID

/**
 * Initial object sent from the client to the server
 */
internal class OrchestrationHandshake(
    val clientId: UUID,
    val clientRole: OrchestrationClientRole
) : Serializable