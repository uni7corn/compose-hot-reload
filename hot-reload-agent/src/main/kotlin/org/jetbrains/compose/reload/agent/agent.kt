@file:JvmName("ComposeHotReloadAgent")

package org.jetbrains.compose.reload.agent

import java.lang.instrument.Instrumentation

fun premain(@Suppress("unused") args: String?, instrumentation: Instrumentation) {
    createPidfile()
    enableComposeHotReloadMode()
    launchComposeGroupInvalidation()
    launchRuntimeTracking(instrumentation)
    launchReloadRequestHandler(instrumentation)
    launchRecompiler()
    launchDevtoolsApplication()
}
