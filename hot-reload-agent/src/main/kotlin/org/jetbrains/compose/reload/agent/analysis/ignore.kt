package org.jetbrains.compose.reload.agent.analysis

import kotlin.text.startsWith

internal fun isIgnoredClassId(classId: String): Boolean = with(classId) {
    startsWith("java/") ||
            startsWith("javax/") ||
            startsWith("java/") ||
            startsWith("jdk/") ||
            startsWith("sun/") ||
            startsWith("kotlin/") ||
            startsWith("kotlinx/") ||
            startsWith("androidx/") ||
            startsWith("org/jetbrains/skia/") ||
            startsWith("org/jetbrains/skiko/")
}