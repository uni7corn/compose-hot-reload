package org.jetbrains.compose.reload.orchestration

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.UUID

public sealed class OrchestrationMessage : Serializable {

    public val messageId: UUID = UUID.randomUUID()

    override fun equals(other: Any?): Boolean {
        if (other !is OrchestrationMessage) return false
        if (other.messageId != this.messageId) return false
        return true
    }

    override fun hashCode(): Int {
        return messageId.hashCode()
    }

    public class GradleDaemonReady : OrchestrationMessage()

    public data class ReloadClassesRequest(
        /**
         * Note: In case of any failure of a reload of classes, the 'pending changes'
         * will be kept by the Agent: This means, that a 'retry' is effectively just a request without
         * additional changed files
         */
        val changedClassFiles: Map<File, ChangeType> = emptyMap()
    ) : OrchestrationMessage() {
        public enum class ChangeType : Serializable {
            Modified, Added, Removed
        }
    }

    public class ShutdownRequest : OrchestrationMessage()

    public data class AgentReloadClassesResult(
        val requestId: UUID,
        val isSuccess: Boolean,
        val errorMessage: String? = null,
    ) : OrchestrationMessage()


    public data class LogMessage(
        val log: String
    ) : OrchestrationMessage()

    public data class UIRendered(
        val reloadRequestId: UUID?,
        val iteration: Int,
    ) : OrchestrationMessage()
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
