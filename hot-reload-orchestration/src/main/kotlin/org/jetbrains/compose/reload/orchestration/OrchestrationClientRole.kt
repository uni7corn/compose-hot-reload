package org.jetbrains.compose.reload.orchestration

import java.io.Serializable

public enum class OrchestrationClientRole : Serializable {
    /**
     * The compose application 'under orchestration'
     */
    Application,

    /**
     * The compiler, which is expected to send updates for .class files:
     * Can be a Build System like Gradle or Amper, or can be the IDE
     */
    Compiler,

    /**
     * Can be any generic client (e.g. tooling which is listening for messages in the orchestration)
     */
    Unknown
}
