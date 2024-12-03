package org.jetbrains.compose.reload.analysis

import org.jetbrains.compose.reload.analysis.MethodIds.Composer.traceEventStart
import org.objectweb.asm.tree.*

internal object RuntimeScopeHasher : RuntimeInstructionAnalyzer {
    override fun analyze(
        context: RuntimeMethodAnalysisContext, instructionNode: AbstractInsnNode
    ) {
        val scope = context.scope ?: return

        if (instructionNode.opcode > 0) {
            scope.pushHash(instructionNode.opcode)
        }

        when (instructionNode) {
            is MethodInsnNode -> {
                scope.pushHash(instructionNode.owner)
                scope.pushHash(instructionNode.name)
                scope.pushHash(instructionNode.desc)
                scope.pushHash(instructionNode.itf)
            }

            is LdcInsnNode -> {
                val nextInstruction = instructionNode.next

                /*
                We want to ignore constants pushed into 'traceEventStart' as this traces will include
                line numbers. Such calls also don't contribute to implementation of the method.
                 */
                if (nextInstruction is MethodInsnNode && MethodId(nextInstruction) == traceEventStart) {
                    return
                }

                scope.pushHash(instructionNode.cst)
            }

            is InvokeDynamicInsnNode -> {
                scope.pushHash(instructionNode.name)
                scope.pushHash(instructionNode.desc)
                scope.pushHash(instructionNode.bsm?.name)
                scope.pushHash(instructionNode.bsm?.owner)
                scope.pushHash(instructionNode.bsm?.tag)
                scope.pushHash(instructionNode.bsm?.desc)
                instructionNode.bsmArgs.forEach { scope.pushHash(it) }
            }

            is FieldInsnNode -> {
                scope.pushHash(instructionNode.owner)
                scope.pushHash(instructionNode.name)
                scope.pushHash(instructionNode.desc)
            }

            is IntInsnNode -> {
                scope.pushHash(instructionNode.operand)
            }

            is VarInsnNode -> {
                scope.pushHash(instructionNode.`var`)
            }
        }
    }
}