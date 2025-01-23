package org.jetbrains.compose.reload.analysis

internal object Ids {
    object Composer {
        val classId = ClassId("androidx/compose/runtime/Composer")

        val startRestartGroup = MethodId(
            classId,
            methodName = "startRestartGroup",
            methodDescriptor = "(I)Landroidx/compose/runtime/Composer;"
        )

        val startReplaceGroup = MethodId(
            classId,
            methodName = "startReplaceGroup",
            methodDescriptor = "(I)V"
        )

        val endReplaceGroup = MethodId(
            classId,
            methodName = "endReplaceGroup",
            methodDescriptor = "()V"
        )

        val endRestartGroup = MethodId(
            classId,
            methodName = "endRestartGroup",
            methodDescriptor = "()Landroidx/compose/runtime/ScopeUpdateScope;"
        )
    }

    object ComposerKt {
        val classId = ClassId("androidx/compose/runtime/ComposerKt")

        val traceEventStart = MethodId(
            classId,
            methodName = "traceEventStart",
            methodDescriptor = "(IIILjava/lang/String;)V"
        )
    }

    object FunctionKeyMeta {
        val classId = ClassId("androidx/compose/runtime/internal/FunctionKeyMeta")
    }
}

val ClassId.classInitializerMethodId: MethodId
    get() = MethodId(this, "<clinit>", "()V")
