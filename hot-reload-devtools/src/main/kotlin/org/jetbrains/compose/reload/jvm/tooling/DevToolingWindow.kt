package org.jetbrains.compose.reload.jvm.tooling

import androidx.compose.ui.window.application
import io.sellmair.evas.compose.installEvas
import io.sellmair.evas.eventsOrThrow
import io.sellmair.evas.statesOrThrow
import org.jetbrains.compose.resources.ExperimentalResourceApi


@OptIn(ExperimentalResourceApi::class)
internal fun runDevToolingApplication() {
    application(
        exitProcessOnExit = false,
    ) {
        installEvas(
            applicationScope.coroutineContext.eventsOrThrow,
            applicationScope.coroutineContext.statesOrThrow
        ) {
            DevOverlays()
        }
    }
}
