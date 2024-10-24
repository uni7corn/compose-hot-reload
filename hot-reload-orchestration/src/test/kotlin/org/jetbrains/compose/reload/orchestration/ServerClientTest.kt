package org.jetbrains.compose.reload.orchestration

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.LogMessage
import org.junit.jupiter.api.AfterEach
import java.util.Collections.synchronizedList
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerClientTest {

    private val resources = mutableListOf<AutoCloseable>()

    fun <T : AutoCloseable> use(value: T): T {
        resources.add(value)
        return value
    }

    @AfterEach
    fun cleanup() {
        resources.forEach { it.close() }
    }

    @Test
    fun `test - simple ping pong`() = runTest {
        val server = use(startOrchestrationServer())
        val client = use(connectOrchestrationClient(server.port))

        val clientMessages = client.asChannel()

        val serverReceivedMessages = mutableListOf<OrchestrationMessage>()
        val clientReceivedMessages = mutableListOf<OrchestrationMessage>()

        server.invokeWhenMessageReceived { message ->
            serverReceivedMessages.add(message)
        }

        client.invokeWhenMessageReceived { message ->
            clientReceivedMessages.add(message)
        }

        client.sendMessage(LogMessage("A")).get()
        clientMessages.receive()

        orchestrationThread.submit {
            assertEquals(listOf(LogMessage("A")), serverReceivedMessages.toList())
            assertEquals(listOf(LogMessage("A")), clientReceivedMessages.toList())
        }.get()
    }


    @Test
    fun `test - single shot request`() = runTest {
        val server = use(startOrchestrationServer())
        val client = use(connectOrchestrationClient(server.port))
        val serverMessages = server.asChannel()

        val serverReceivedMessages = synchronizedList(mutableListOf<OrchestrationMessage>())
        val clientReceivedMessages = synchronizedList(mutableListOf<OrchestrationMessage>())

        server.invokeWhenMessageReceived { message ->
            serverReceivedMessages.add(message)
        }

        client.invokeWhenMessageReceived { message ->
            clientReceivedMessages.add(message)
        }

        client.use { client ->
            client.sendMessage(LogMessage("A"))
        }

        serverMessages.receive()
        assertEquals(listOf(LogMessage("A")), serverReceivedMessages)
        assertEquals(listOf(), clientReceivedMessages)
    }


    @Test
    fun `test - multiple messages`() = runTest {
        val server = use(startOrchestrationServer())
        val client = use(connectOrchestrationClient(server.port))
        val serverChannel = server.asChannel()
        val clientChannel = client.asChannel()

        launch {
            val firstLog = serverChannel.receiveAsFlow().filterIsInstance<LogMessage>().first()
            assertEquals("A", firstLog.log)

            val secondLog = serverChannel.receiveAsFlow().filterIsInstance<LogMessage>().first()
            assertEquals("B", secondLog.log)
        }


        launch {
            val firstLog = clientChannel.receiveAsFlow().filterIsInstance<LogMessage>().first()
            assertEquals("A", firstLog.log)

            val secondLog = clientChannel.receiveAsFlow().filterIsInstance<LogMessage>().first()
            assertEquals("B", secondLog.log)
        }

        client.sendMessage(LogMessage("A"))
        client.sendMessage(LogMessage("B"))
    }
}

