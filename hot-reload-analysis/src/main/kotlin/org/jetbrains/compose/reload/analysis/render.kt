package org.jetbrains.compose.reload.analysis

import org.jetbrains.compose.reload.analysis.RuntimeScopeType.Method
import org.jetbrains.compose.reload.analysis.RuntimeScopeType.ReplaceGroup
import org.jetbrains.compose.reload.analysis.RuntimeScopeType.RestartGroup
import org.jetbrains.compose.reload.analysis.RuntimeScopeType.SourceInformationMarker


internal fun RuntimeInfo.render(): String = buildString {
    scopes.groupBy { it.methodId.classId }.toSortedMap().forEach { (className, scopes) ->
        appendLine("$className {")
        withIndent {
            appendLine(scopes.joinToString("\n\n") { it.render() })
        }
        appendLine("}")
        appendLine()
    }
}


internal fun RuntimeScopeInfo.render(): String = buildString {
    when (type) {
        Method -> appendLine("${methodId.methodName} {")
        RestartGroup -> appendLine("RestartGroup {")
        ReplaceGroup -> appendLine("ReplaceGroup {")
        SourceInformationMarker -> appendLine("SourceInformationMarker {")
    }

    withIndent {
        if (type == Method) {
            appendLine("desc: ${methodId.methodDescriptor}")
        }

        appendLine("key: ${group?.key}")
        appendLine("codeHash: ${hash.value}")
        if (dependencies.isEmpty()) {
            appendLine("dependencies: []")
        } else {
            appendLine("dependencies: [")
            withIndent {
                append(dependencies.joinToString(",\n"))
            }
            appendLine("]")
        }

        if (children.isNotEmpty()) {
            appendLine()
            appendLine(children.joinToString("\n\n") { it.render() }).trim()
            appendLine()
        }
    }

    append("}")
}

fun StringBuilder.withIndent(builder: StringBuilder.() -> Unit) {
    appendLine(buildString(builder).trim().prependIndent("    "))
}