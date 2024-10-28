package org.jetbrains.compose.reload.orchestration

import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
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

fun OrchestrationMessage.encodeToByteArray(): ByteArray {
    return ByteArrayOutputStream().use { baos ->
        ObjectOutputStream(baos).use { oos -> oos.writeObject(this) }
        baos.toByteArray()
    }
}

fun decodeOrchestrationMessage(bytes: ByteArray): OrchestrationMessage {
    return ObjectInputStream(bytes.inputStream()).use { ois ->
        ois.readObject() as OrchestrationMessage
    }
}