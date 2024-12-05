package org.jetbrains.compose.reload.agent.tests

import javassist.ClassPool
import javassist.Modifier
import org.jetbrains.compose.reload.agent.transformForStaticsInitialization
import org.jetbrains.compose.reload.core.testFixtures.Compiler
import org.jetbrains.compose.reload.core.testFixtures.WithCompiler
import org.jetbrains.compose.reload.analysis.testFixtures.checkJavap
import org.jetbrains.compose.reload.core.testFixtures.compile
import org.junit.jupiter.api.TestInfo
import kotlin.test.Test
import kotlin.test.assertEquals

@WithCompiler
class StaticsInitializationTest() {

    private val pool = ClassPool(true)
    private val loader = javassist.Loader.Simple()

    @Test
    fun `test - adding top level val`(compiler: Compiler, info: TestInfo) {
        val beforeCode = compiler.compile(
            "Test.kt" to """
             val empty = 42
            """.trimIndent()
        ).getValue("TestKt.class")
        val beforeCtClazz = pool.makeClass(beforeCode.inputStream())
        val beforeClazz = beforeCtClazz.toClass(loader, javaClass.protectionDomain)
        beforeCtClazz.defrost()

        val afterBytecode = compiler.compile(
            "Test.kt" to """
            val empty = 42
            val foo = 42
            """.trimIndent()
        )

        val afterCtClazz = pool.makeClass(afterBytecode.getValue("TestKt.class").inputStream())
        afterCtClazz.transformForStaticsInitialization(beforeClazz)

        assertEquals(
            0, afterCtClazz.getDeclaredField("foo").modifiers and Modifier.FINAL,
            "Expected 'val foo' to not be static anymore"
        )
        checkJavap(info, code = mapOf("FooKt.class" to afterCtClazz.toBytecode()))
    }

    @Test
    fun `test - no field added - results in no reinitilizer being generated`(compiler: Compiler, info: TestInfo) {
        val beforeCode = compiler.compile(
            "Test.kt" to """
             val empty = 42
            """.trimIndent()
        ).getValue("TestKt.class")
        val beforeCtClazz = pool.makeClass(beforeCode.inputStream())
        val beforeClazz = beforeCtClazz.toClass(loader, javaClass.protectionDomain)
        beforeCtClazz.defrost()

        val afterBytecode = compiler.compile(
            "Test.kt" to """
            val empty = 42
            fun foo() = Unit
            """.trimIndent()
        )

        val afterCtClazz = pool.makeClass(afterBytecode.getValue("TestKt.class").inputStream())
        afterCtClazz.transformForStaticsInitialization(beforeClazz)

        checkJavap(info, code = mapOf("FooKt.class" to afterCtClazz.toBytecode()))
    }
}
