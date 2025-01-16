package org.jetbrains.compose.reload.analysis

data class RuntimeScopeInfo(
    val methodId: MethodId,
    val tree: RuntimeInstructionTree,
    val methodDependencies: Set<MethodId>,
    val fieldDependencies: Set<FieldId>,
    val children: List<RuntimeScopeInfo>,
    val hash: RuntimeInstructionTreeCodeHash,
)
