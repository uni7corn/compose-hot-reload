/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.compose.reload.core

import java.io.File
import java.lang.System.getProperty
import java.nio.file.Path
import kotlin.io.path.Path

public fun system(property: HotReloadProperty): String? = getProperty(property.key)

public fun systemBoolean(property: HotReloadProperty, default: Boolean): Boolean =
    getProperty(property.key)?.toBooleanStrict() ?: default

public fun systemInt(property: HotReloadProperty): Int? = getProperty(property.key)?.toIntOrNull()

public fun systemFiles(property: HotReloadProperty): List<Path>? =
    getProperty(property.key)?.split(File.pathSeparator)?.map(::Path)

public inline fun <reified T : Enum<T>> systemEnum(property: HotReloadProperty): T? =
    getProperty(property.key)?.let { enum -> return enumValueOf<T>(enum) }

public inline fun <reified T : Enum<T>> systemEnum(property: HotReloadProperty, defaultValue: T): T =
    systemEnum<T>(property) ?: defaultValue

public fun environment(property: HotReloadProperty): String? = System.getenv(property.key)

public fun environment(property: HotReloadProperty, default: String): String = environment(property) ?: default

public fun environmentInt(property: HotReloadProperty): Int? = environment(property)?.toInt()
