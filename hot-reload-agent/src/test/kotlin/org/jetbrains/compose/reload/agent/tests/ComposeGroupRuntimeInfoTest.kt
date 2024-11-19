package org.jetbrains.compose.reload.agent.tests

import org.jetbrains.compose.reload.agent.ComposeGroupKey
import org.jetbrains.compose.reload.agent.parseComposeGroupRuntimeInfos
import org.jetbrains.compose.reload.agent.utils.Compiler
import org.jetbrains.compose.reload.agent.utils.WithCompiler
import org.jetbrains.compose.reload.agent.utils.compile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.fail

@WithCompiler
class ComposeGroupRuntimeInfoTest() {

    @Test
    fun `test - add remembered value`(compiler: Compiler) {
        val (group, runtimeInfo) = run {
            val output = compiler.compile(
                "Foo.kt" to """
                import androidx.compose.runtime.Composable
                
                @Composable
                fun Foo() {
                }
            """.trimIndent()
            )

            val composeGroups = parseComposeGroupRuntimeInfos(output["FooKt.class"]!!)
            assertEquals(1, composeGroups.size, "Expected only a single compose group")

            val (group, runtimeInfo) = composeGroups.entries.single()
            assertEquals("FooKt.Foo", runtimeInfo.callSiteMethodFqn)
            assertEquals(ComposeGroupKey(-965539098), group) // Hardcoded; Would be fine to change, but interesting
            group to runtimeInfo
        }


        run {
            val output = compiler.compile(
                "Foo.kt" to """
                import androidx.compose.runtime.*
                
                @Composable
                fun Foo() {
                    remember { 42 }
                }
            """.trimIndent()
            )

            val composeGroups = parseComposeGroupRuntimeInfos(output["FooKt.class"]!!)
            assertEquals(1, composeGroups.size, "Expected only a single compose group")

            val (groupAfter, runtimeInfoAfter) = composeGroups.entries.single()
            assertEquals(group, groupAfter, "Expected group to be un-affected by change")
            assertNotEquals(
                runtimeInfo.invalidationKey, runtimeInfoAfter.invalidationKey,
                "Expected different 'invalidationKey' after adding remember block"
            )
        }
    }

    @Test
    fun `test - change remembered constant`(compiler: Compiler) {
        val outputBefore = compiler.compile(
            "Foo.kt" to """
                import androidx.compose.runtime.*
                
                @Composable
                fun Foo() {
                    remember { 42 }
                }
            """.trimIndent()
        )

        val outputAfter = compiler.compile(
            "Foo.kt" to """
                import androidx.compose.runtime.*
                
                @Composable
                fun Foo() {
                    remember { 43 } // <- was 42 before!
                }
            """.trimIndent()
        )

        val groupsBefore = parseComposeGroupRuntimeInfos(outputBefore["FooKt.class"]!!)
        val groupsAfter = parseComposeGroupRuntimeInfos(outputAfter["FooKt.class"]!!)

        if (groupsBefore.entries.size != 1) {
            fail("Expected single group; Found $groupsBefore")
        }

        if (groupsAfter.entries.size != 1) {
            fail("Expected single group; Found $groupsAfter")
        }

        val groupBefore = groupsBefore.entries.single()
        val groupAfter = groupsAfter.entries.single()

        assertEquals(groupBefore.key, groupAfter.key, "Expected matching keys for 'Foo'")
        assertNotEquals(
            groupBefore.value.invalidationKey, groupAfter.value.invalidationKey,
            "Expected 'invalidationKey' for 'Foo'"
        )
    }

    @Test
    fun `test - change outside of remember block`(compiler: Compiler) {
        val outputBefore = compiler.compile(
            "Foo.kt" to """
            import androidx.compose.runtime.*
                
                @Composable
                fun Foo() {
                    println("Hello")
                    remember { 42 }
                }
            """.trimIndent()
        )

        val outputAfter = compiler.compile(
            "Foo.kt" to """
            import androidx.compose.runtime.*
                
                @Composable
                fun Foo() {
                    println("Servus") // <- Was "Hello" before!
                    remember { 42 }
                }
            """.trimIndent()
        )

        assertEquals(
            parseComposeGroupRuntimeInfos(outputBefore["FooKt.class"]!!),
            parseComposeGroupRuntimeInfos(outputAfter["FooKt.class"]!!)
        )
    }

    @Test
    fun `test - change in lambda`(compiler: Compiler) {
        val outputBefore = compiler.compile(
            "Foo.kt" to """
                import androidx.compose.runtime.*
                
                @Composable
                fun Bar(child: @Composable () -> Unit) {
                    child()
                }
                
                @Composable
                fun Foo() {
                    Bar {
                        var x = remember { 42 }
                        print(x)
                    }
                }
            """.trimIndent()
        )

        val outputAfter = compiler.compile(
            "Foo.kt" to """
                import androidx.compose.runtime.*
                
                @Composable
                fun Bar(child: @Composable () -> Unit) {
                    child()
                }
                
                @Composable
                fun Foo() {
                    Bar {
                        var x = remember { 43 } // <- was 42 before!
                        print(x)
                    }
                }
            """.trimIndent()
        )

        val composableLambdaBefore = outputBefore.entries.single { (className, _) ->
            className.matches(Regex(""".*lambda.*\$1\.class"""))
        }

        val composableLambdaAfter = outputAfter.entries.single { (className, _) ->
            className.matches(Regex(""".*lambda.*\$1\.class"""))
        }

        val runtimeInfoBefore = parseComposeGroupRuntimeInfos(composableLambdaBefore.value)
            .entries.single()

        val runtimeInfoAfter = parseComposeGroupRuntimeInfos(composableLambdaAfter.value)
            .entries.single()

        assertNotEquals(
            runtimeInfoBefore.value.invalidationKey, runtimeInfoAfter.value.invalidationKey,
            "Expected changed invalidation keys."
        )
    }
}
