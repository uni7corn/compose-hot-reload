package org.jetbrains.compsoe.reload.analyzer.app.events

import io.sellmair.evas.Event
import java.nio.file.Path

data class RegularFileOpenEvent(val path: Path) : Event