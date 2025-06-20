import org.jetbrains.compose.reload.core.Future
import org.jetbrains.compose.reload.core.complete
import org.jetbrains.compose.reload.core.completeExceptionally
import org.jetbrains.compose.reload.core.getOrThrow
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.check
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingOptions
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions
import org.jetbrains.kotlinx.lincheck.util.LoggingLevel
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test

/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

class FutureLincheck {
    val future = Future<Int>()

    @Operation
    fun complete(value: Int) {
        future.complete(value)
    }

    @Operation
    fun completeWithException() {
        future.completeExceptionally(Throwable())
    }

    @Operation
    fun isCompleted(): Boolean {
        return future.isCompleted()
    }

    @Operation
    suspend fun await(): Int {
        return future.await().getOrThrow()
    }

    @Operation
    suspend fun awaitWithContinuation(): Int {
        return suspendCoroutine<Int> {
            future.awaitWith(it)
        }
    }

    @EnabledIfSystemProperty(named = "lincheck", matches = "true")
    @Test
    fun modelTest() {
        ModelCheckingOptions()
            .logLevel(LoggingLevel.INFO)
            .actorsBefore(0)
            .check(this::class)
    }

    @EnabledIfSystemProperty(named = "lincheck", matches = "true")
    @Test
    fun stressTest() {
        StressOptions().check(this::class)
    }
}
