/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

@file:Suppress("unused")

package tests

import org.jetbrains.compose.reload.DelicateHotReloadApi
import org.jetbrains.compose.reload.staticHotReloadScope
import org.jetbrains.compose.reload.test.HotReloadUnitTest
import org.jetbrains.compose.reload.test.compileAndReload
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

private object Counter {
    var counter = AtomicInteger()
}

@OptIn(DelicateHotReloadApi::class)
@HotReloadUnitTest
fun `test - invokeAfterHotReload`() {
    val invocations = AtomicInteger(0)

    val registration = staticHotReloadScope.invokeAfterHotReload {
        invocations.incrementAndGet()
    }

    compileAndReload(
        """
        object Decoy {
            val decoy = 42
        }
    """.trimIndent()
    )
    assertEquals(1, invocations.get())

    compileAndReload(
        """
        object Decoy {
            val decoy = 43
        }
    """.trimIndent()
    )
    assertEquals(2, invocations.get())

    registration.close()
    compileAndReload(
        """
        object Decoy {
            val decoy = 44
        }
    """.trimIndent()
    )
    assertEquals(2, invocations.get())
}

@HotReloadUnitTest
fun `test - removing invokeAfterHotReload code`() {
    // Add invokeAfterHotReload 'after the fact'
    compileAndReload(
        """
        package tests
        import org.jetbrains.compose.reload.staticHotReloadScope
        import java.util.concurrent.atomic.AtomicInteger
        
        private object Counter {
            var counter = AtomicInteger(0).apply {
                staticHotReloadScope.invokeAfterHotReload {
                    incrementAndGet()
                 }
            }
        }
    """.trimIndent()
    )
    assertEquals(1, Counter.counter.get())

    /* Reload a decoy */
    compileAndReload(
        """
        object Decoy {
            val decoy = 42
        }
    """.trimIndent()
    )
    assertEquals(2, Counter.counter.get())

    /* Reload a decoy */
    compileAndReload(
        """
        object Decoy {
            val decoy = 44
        }
    """.trimIndent()
    )
    assertEquals(3, Counter.counter.get())

    /* Remove listener */
    compileAndReload(
        """
        package tests
        import org.jetbrains.compose.reload.staticHotReloadScope
        import java.util.concurrent.atomic.AtomicInteger
        
        private object Counter {
            var counter = AtomicInteger(-1)
        }
    """.trimIndent()
    )
    assertEquals(-1, Counter.counter.get())

    /* Check decoy not affecting counter anymore */
    /* Reload a decoy */
    compileAndReload(
        """
        object Decoy {
            val decoy = 45
        }
    """.trimIndent()
    )
    assertEquals(-1, Counter.counter.get())
}
