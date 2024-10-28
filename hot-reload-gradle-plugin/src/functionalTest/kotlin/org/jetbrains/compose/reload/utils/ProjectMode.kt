package org.jetbrains.compose.reload.utils

import org.junit.jupiter.api.extension.ExtensionContext

enum class ProjectMode {
    Kmp, Jvm
}

var ExtensionContext.projectMode: ProjectMode? by extensionContextProperty()