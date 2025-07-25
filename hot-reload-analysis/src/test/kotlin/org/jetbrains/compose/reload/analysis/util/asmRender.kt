/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.compose.reload.analysis.util

import org.jetbrains.compose.reload.analysis.ClassNode
import org.jetbrains.compose.reload.analysis.InstructionToken
import org.jetbrains.compose.reload.analysis.InstructionTree
import org.jetbrains.compose.reload.analysis.indent
import org.jetbrains.compose.reload.analysis.parseInstructionTree
import org.jetbrains.compose.reload.analysis.plusAssign
import org.jetbrains.compose.reload.analysis.tokenizeInstructions
import org.jetbrains.compose.reload.analysis.withIndent
import org.jetbrains.compose.reload.core.leftOr
import org.jetbrains.compose.reload.core.withClosure
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor
import java.io.PrintWriter
import java.io.StringWriter


internal fun renderAsmTree(bytecode: ByteArray): String {
    return ClassNode(bytecode).renderAsmTree()
}

internal fun ClassNode.renderAsmTree(): String = buildString {
    appendLine("class ${name.replace("/", ".")} {")
    withIndent {
        methods.sortedBy { node -> node.name + node.desc }.forEachIndexed { index, methodNode ->
            val methodTokens = tokenizeInstructions(methodNode.instructions.toList())
                .leftOr { error("Failed to tokenize a method: $it") }
            val tree = parseInstructionTree(methodNode, methodTokens)
                .leftOr { error("Failed to parse runtime instruction tree: $it") }

            methodNode.verifyTree(methodTokens, tree)

            this += "fun ${methodNode.name} ${methodNode.desc} {"
            this += methodNode.renderAsm(tree).indent()
            this += "}"
            if (index != methods.lastIndex) {
                appendLine()
            }
        }
    }
    appendLine("}")
}

internal fun MethodNode.verifyTree(methodTokens: List<InstructionToken>, methodTree: InstructionTree) {
    val treeNodes = methodTree.withClosure { it.children }

    val treeTokens = treeNodes.flatMapTo(mutableSetOf()) { it.tokens }
    // labels after last 'return' instruction are not part of the tree
    for (token in methodTokens.dropLastWhile { it is InstructionToken.LabelToken }) {
        if (token !in treeTokens) {
            error("Token $token is not a part of the final instruction tree")
        }
    }

    val treeInstructions = treeNodes.flatMap { node -> node.tokens.flatMap { it.instructions } }
    // labels after last 'return' instruction are not part of the tree
    for (instruction in instructions.toList().dropLastWhile { it is LabelNode }) {
        if (instruction !in treeInstructions) {
            error("Instruction $instruction is not a part of the final instruction tree")
        }
    }
}

internal fun InstructionTree.inst2TreeMapping(): Map<AbstractInsnNode, InstructionTree> {
    val treeNodes = this.withClosure { it.children }
    val inst2Tree = mutableMapOf<AbstractInsnNode, InstructionTree>()
    treeNodes.forEach { node ->
        node.tokens.forEach { token ->
            token.instructions.forEach { inst ->
                inst2Tree[inst] = node
            }
        }
    }
    return inst2Tree
}

internal fun InstructionTree.tree2IndentMapping(): Map<InstructionTree, Int> {
    val treeIndent = mutableMapOf<InstructionTree, Int>()
    treeIndent[this] = 0

    val queue = ArrayDeque<InstructionTree>()
    queue.addLast(this)
    while (queue.isNotEmpty()) {
        val current = queue.removeLast()
        val currentDepth = treeIndent[current] ?: 0
        for (child in current.children) {
            treeIndent[child] = currentDepth + 1
            queue.addLast(child)
        }
    }
    return treeIndent
}

internal fun MethodNode.renderAsm(tree: InstructionTree) = buildString {
    val inst2Tree = tree.inst2TreeMapping()
    val treeIndent = tree.tree2IndentMapping()

    val printer = Textifier()
    val mp = TraceMethodVisitor(printer)

    this += tree.render()

    var prevTree: InstructionTree? = tree
    for (inst in instructions) {
        val currentTree = inst2Tree[inst]
        val indent = treeIndent[currentTree] ?: 0

        if (currentTree != null && prevTree != currentTree) {
            appendLine("${currentTree.type} (group=${currentTree.group})".indent(indent))
        }

        appendLine(inst.print(printer, mp).indent(indent))
        prevTree = currentTree
    }
    this += "}"
}.trim()

internal fun AbstractInsnNode.print(printer: Textifier, mp: TraceMethodVisitor): String {
    this.accept(mp as MethodVisitor)
    val sw = StringWriter()
    printer.print(PrintWriter(sw))
    printer.getText().clear()
    return sw.toString().trim()
}

internal fun InstructionTree.render(): String = "$type (group=${group})"
