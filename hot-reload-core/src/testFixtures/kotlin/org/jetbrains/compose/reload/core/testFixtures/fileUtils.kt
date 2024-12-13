package org.jetbrains.compose.reload.core.testFixtures

fun String.sanitized(): String {
    return lines().joinToString("\n").trim()
}
