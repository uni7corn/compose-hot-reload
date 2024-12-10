@file:JvmName("Main")

package org.jetbrains.compose.reload.jvm.tooling

import io.sellmair.evas.Events
import io.sellmair.evas.States
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.compose.reload.jvm.tooling.states.launchConsoleLogState
import org.jetbrains.compose.reload.jvm.tooling.states.launchUIErrorState
import org.jetbrains.compose.reload.jvm.tooling.states.launchWindowsState

val applicationScope = CoroutineScope(Dispatchers.Main + SupervisorJob() + Events() + States())

fun main() {
    applicationScope.launchConsoleLogState()
    applicationScope.launchWindowsState()
    applicationScope.launchUIErrorState()
    runDevToolingApplication()
}
