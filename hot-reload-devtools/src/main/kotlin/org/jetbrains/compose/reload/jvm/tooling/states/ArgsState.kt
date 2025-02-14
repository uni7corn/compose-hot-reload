/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.compose.reload.jvm.tooling.states

import io.sellmair.evas.State
import io.sellmair.evas.launchState
import io.sellmair.evas.set
import kotlinx.coroutines.CoroutineScope

data class DtArguments(
    val originalApplicationCommand: String?,
    val originalApplicationArguments: List<String>
) : State {
    companion object Key : State.Key<DtArguments?> {
        override val default: DtArguments? = null
    }
}

fun CoroutineScope.launchDtArgumentsState(args: List<String>) = launchState(DtArguments.Key) {
    DtArguments.set(parseArguments(args))
}

private fun parseArguments(args: List<String>): DtArguments {
    var originalApplicationCommand: String? = null
    val originalApplicationArguments = mutableListOf<String>()

    args.forEach { arg ->
        when {
            arg.startsWith("--applicationCommand=") -> {
                originalApplicationCommand = arg.substringAfter("=")
            }

            arg.startsWith("--applicationArg=") -> {
                originalApplicationArguments.add(arg.substringAfter("="))
            }

            else -> error("Unknown argument: $arg")
        }
    }

    return DtArguments(
        originalApplicationCommand = originalApplicationCommand,
        originalApplicationArguments = originalApplicationArguments
    )
}
