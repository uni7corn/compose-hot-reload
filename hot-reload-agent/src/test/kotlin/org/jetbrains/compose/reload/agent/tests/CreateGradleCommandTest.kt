package org.jetbrains.compose.reload.agent.tests

import org.jetbrains.compose.reload.agent.createGradleCommand
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateGradleCommandTest {
    @Test
    fun `test root project`() {
        assertEquals(":foo", createGradleCommand(":", "foo"))
    }

    @Test
    fun `test subproject`() {
        assertEquals(":sub:foo", createGradleCommand(":sub", "foo"))
    }
}