package org.jetbrains.compose.reload.jvm

internal object HotReloadEnvironment {
    val isHeadless = System.getProperty("compose.reload.headless")?.toBoolean() == true
}