package org.jetbrains.compose.reload.jvm

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

@Suppress("NOTHING_TO_INLINE") // We want the caller class!
@JvmName("createLookupLogger")
internal inline fun createLogger(): Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
