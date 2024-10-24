package org.jetbrains.compose.reload.orchestration

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class SerializationTest {
    @Test
    fun `test - reload request`() {
        val request = OrchestrationMessage.ReloadClassesRequest(
            mapOf(File("my/file") to OrchestrationMessage.ReloadClassesRequest.ChangeType.Modified)
        )

        assertEquals(
            expected = request, actual = decodeOrchestrationMessage(request.encodeToByteArray())
        )
    }
}