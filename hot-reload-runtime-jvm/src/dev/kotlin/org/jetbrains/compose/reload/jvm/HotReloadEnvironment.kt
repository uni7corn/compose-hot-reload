@file:Suppress("NullableBooleanElvis")

package org.jetbrains.compose.reload.jvm

internal object HotReloadEnvironment {
    val isHeadless = System.getProperty("compose.reload.headless")?.toBoolean() == true
    val showDevTooling = System.getProperty("compose.reload.showDevTooling")?.toBoolean() ?: true
}
