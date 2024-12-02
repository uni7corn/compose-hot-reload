package org.jetbrains.compose.reload.agent.analysis

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodInsnNode

internal object RuntimeScopeComposeGroupAnalyzer : RuntimeInstructionAnalyzer {
    override fun analyze(
        context: RuntimeMethodAnalysisContext, instructionNode: AbstractInsnNode
    ) {
        if (instructionNode !is MethodInsnNode) return

        when (MethodId(instructionNode)) {
            /* Restart Groups */
            MethodIds.Composer.startRestartGroup -> context.pushStartRestartGroup(
                ComposeGroupKey(instructionNode.previous.intValueOrNull() ?: return)
            )

            MethodIds.Composer.endRestartGroup -> context.popEndRestartGroup()


            /* Replace Groups */
            MethodIds.Composer.startReplaceGroup -> context.pushStartReplaceGroup(
                ComposeGroupKey(instructionNode.previous.intValueOrNull() ?: return)
            )

            MethodIds.Composer.endReplaceGroup -> context.popEndReplaceGroup()

            /* Source Information Marker */

            MethodIds.Composer.sourceInformationMarkerStart -> Unit /*context.pushSourceInformationMarkerStart(
                ComposeGroupKey(instructionNode.previous.intValueOrNull() ?: return)
            ) */

            MethodIds.Composer.sourceInformationMarkerEnd -> Unit //context.popSourceInformationMarkerEnd()
            else -> Unit
        }
    }
}