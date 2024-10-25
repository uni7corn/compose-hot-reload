package org.jetbrains.compose.reload.orchestration

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.UUID

public sealed class OrchestrationMessage : Serializable {
    /**
     * Requests that all participants in the orchestration are supposed to shut down.
     * Note: Closing the [OrchestrationServer] is also supposed to close all clients.
     */
    public class ShutdownRequest : OrchestrationMessage()

    /**
     * Indicates that the 'Gradle Daemon' which is listening for changed source code, then recompiling is ready.
     */
    public class GradleDaemonReady : OrchestrationMessage()

    /**
     * Signals to the application (agent) that it should reload the provided classes.
     *
     * Note: In case of any failure of a reload of classes, the 'pending changes'
     * will be kept by the Agent: This means, that a 'retry' is effectively just a request without
     * additional changed files
     */
    public data class ReloadClassesRequest(
        val changedClassFiles: Map<File, ChangeType> = emptyMap()
    ) : OrchestrationMessage() {
        public enum class ChangeType : Serializable {
            Modified, Added, Removed
        }
    }

    /**
     * Requests the client to take (and send) a screenshot:
     * Note, only special clients (e.g. clients under test) do support this.
     */
    public class TakeScreenshotRequest : OrchestrationMessage()

    /**
     * A screenshot was taken (as response to [TakeScreenshotRequest])
     * @param format the image format (for example, 'png')
     * @param data the raw image data)
     */
    public class Screenshot(
        public val format: String,
        public val data: ByteArray
    ) : OrchestrationMessage()

    /**
     * Simple message that can be passed across the whole orchestration:
     * Can be used for very important log messages, or for testing.
     */
    public data class LogMessage(
        val log: String
    ) : OrchestrationMessage()

    /**
     * Sent once the UI was rendered
     * @param reloadRequestId: The uuid of the [ReloadClassesRequest] which caused the rendering
     * (or null for the initial renderings)
     *
     * @param iteration How often was the UI already re-rendered
     */
    public data class UIRendered(
        val reloadRequestId: UUID?,
        val iteration: Int,
    ) : OrchestrationMessage()

    /* Base implementation */

    public val messageId: UUID = UUID.randomUUID()

    override fun equals(other: Any?): Boolean {
        if (other !is OrchestrationMessage) return false
        if (other.messageId != this.messageId) return false
        return true
    }

    override fun hashCode(): Int {
        return messageId.hashCode()
    }
}

public fun OrchestrationMessage.encodeToByteArray(): ByteArray {
    return ByteArrayOutputStream().use { baos ->
        ObjectOutputStream(baos).use { oos -> oos.writeObject(this) }
        baos.toByteArray()
    }
}

public fun decodeOrchestrationMessage(bytes: ByteArray): OrchestrationMessage {
    return ObjectInputStream(bytes.inputStream()).use { ois ->
        ois.readObject() as OrchestrationMessage
    }
}
