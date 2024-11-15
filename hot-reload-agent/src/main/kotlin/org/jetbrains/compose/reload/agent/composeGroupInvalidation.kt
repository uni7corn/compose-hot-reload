package org.jetbrains.compose.reload.agent

import javassist.ClassPool
import javassist.CtMethod
import kotlinx.coroutines.Dispatchers
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.CRC32
import kotlin.coroutines.EmptyCoroutineContext

private val logger = createLogger()

private val instrumentationPool by lazy {
    ClassPool(true)
}

private val composerClazz by lazy {
    instrumentationPool.get("androidx.compose.runtime.Composer")
}

private val startReplaceGroupMethod by lazy {
    composerClazz.getDeclaredMethod(
        "startReplaceGroup",
        arrayOf(instrumentationPool.get(Int::class.javaPrimitiveType!!.name))
    )
}

private val startRestartGroupMethod by lazy {
    composerClazz.getDeclaredMethod(
        "startRestartGroup",
        arrayOf(instrumentationPool.get(Int::class.javaPrimitiveType!!.name))
    )
}

private val endReplaceGroupMethod by lazy {
    composerClazz.getDeclaredMethod("endReplaceGroup")
}

private val endRestartGroupMethod by lazy {
    composerClazz.getDeclaredMethod("endRestartGroup")
}

private val composerRememberedValueMethod by lazy {
    composerClazz.getDeclaredMethod("rememberedValue")
}

private val composerUpdateRememberedValueMethod by lazy {
    composerClazz.getDeclaredMethod(
        "updateRememberedValue",
        arrayOf(instrumentationPool.get(Any::class.java.name))
    )
}

/**
 * The actual key emitted by the Compose Compiler associated with the group.
 * For example, a call like
 * ```kotlin
 * startRestartGroup(1902)
 *```
 *
 * will have the value '1902' recorded as [ComposeGroupKey].
 */
@JvmInline
internal value class ComposeGroupKey(val key: Int)

/**
 * The runtime 'group invalidation key'. This value is calculated purely at runtime.
 * If this key changes after reloading the class, then we know that the group requires invalidation.
 */
@JvmInline
internal value class ComposeGroupInvalidationKey(val key: Int)

/**
 * Runtime information for a given Compose group:
 * @param callSiteMethodFqn: The fqn of the callsite function.
 * ```kotlin
 * // App.kt
 * package foo
 * @Composable fun Bar() {
 *     startRestartGroup(1902) // <- callSiteMethodFqn = foo.AppKt.Bar
 * }
 * ```
 *
 * @param invalidationKey: See [ComposeGroupInvalidationKey]
 */
internal data class ComposeGroupRuntimeInfo(
    val callSiteMethodFqn: String,
    val invalidationKey: ComposeGroupInvalidationKey,
)

private val globalComposeComposeGroupRuntimeInfo = ConcurrentHashMap<ComposeGroupKey, ComposeGroupRuntimeInfo>()

private val globalInvalidComposeGroupKeys = ConcurrentHashMap<ComposeGroupKey, ComposeGroupRuntimeInfo>()

internal fun startComposeGroupInvalidationTransformation(instrumentation: Instrumentation) {
    /*
    Instruct Compose to invalidate groups that have changed, after successful reload.
     */
    ComposeHotReloadAgent.invokeAfterReload { reloadRequestId, error ->
        if (error != null) return@invokeAfterReload

        val invalidations = globalInvalidComposeGroupKeys.entries.toList()
        globalInvalidComposeGroupKeys.clear()

        Dispatchers.Main.dispatch(EmptyCoroutineContext) {
            if (invalidations.isEmpty()) {
                logger.orchestration("All groups retained")
            }

            invalidations.forEach { (groupKey, groupRuntimeInfo) ->
                logger.orchestration("Invalidating group at '${groupRuntimeInfo.callSiteMethodFqn}' ('$groupKey')")
                invalidateGroupsWithKey(groupKey)
            }
        }
    }

    /*
    Register the transformer which will be invoked on all byte-code updating the global group information
     */
    instrumentation.addTransformer(ComposeGroupInvalidationKeyTransformer)
}

/*
This transformer is intended to run on all classes, so that runtime information about Compose groups
is recorded and invalidations can be tracked.
 */
internal object ComposeGroupInvalidationKeyTransformer : ClassFileTransformer {
    override fun transform(
        loader: ClassLoader?, className: String?, classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?, classfileBuffer: ByteArray
    ): ByteArray? {
        try {
            refreshComposeGroupRuntimeInfos(classBeingRedefined, classfileBuffer)
        } catch (t: Throwable) {
            logger.error("Failed refreshing compose group invalidation keys for '$className'", t)
        }

        return classfileBuffer
    }
}

internal fun refreshComposeGroupRuntimeInfos(
    classBeingRedefined: Class<*>?,
    classfileBuffer: ByteArray
) {
    val keys = parseComposeGroupRuntimeInfos(classfileBuffer)
    keys.forEach { groupKey, groupRuntimeInfo ->
        val previousRuntimeInfo = globalComposeComposeGroupRuntimeInfo.put(groupKey, groupRuntimeInfo)
        if (
            classBeingRedefined != null &&
            previousRuntimeInfo != null && previousRuntimeInfo.invalidationKey != groupRuntimeInfo.invalidationKey
        ) {
            globalInvalidComposeGroupKeys.put(groupKey, groupRuntimeInfo)
        }
    }
}

private fun parseComposeGroupRuntimeInfos(classFile: ByteArray): Map<ComposeGroupKey, ComposeGroupRuntimeInfo> {
    val reader = ClassReader(classFile)
    val composeGroupRuntimeInfoBuilder = ComposeGroupRuntimeInfoBuilder()
    reader.accept(composeGroupRuntimeInfoBuilder, 0)
    return composeGroupRuntimeInfoBuilder.groups
}

private class ComposeGroupRuntimeInfoBuilder() : ClassVisitor(ASM9) {
    val groups = hashMapOf<ComposeGroupKey, ComposeGroupRuntimeInfo>()

    private var className: String? = null

    override fun visit(
        version: Int, access: Int, name: String?,
        signature: String?, superName: String?, interfaces: Array<out String?>?
    ) {
        this.className = name
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int, name: String?, descriptor: String?, signature: String?, exceptions: Array<out String?>?
    ): MethodVisitor? = ReloadKeyMethodVisitor(
        callSiteMethodFqn = "$className.$name", groups = groups,
    )
}

private class ReloadKeyMethodVisitor(
    private val callSiteMethodFqn: String,
    private val groups: MutableMap<ComposeGroupKey, ComposeGroupRuntimeInfo>
) : MethodVisitor(ASM9) {

    private var lastIntValue = 0

    private val stack = ArrayDeque<ComposeGroupRuntimeInfoBuilder>()

    inner class ComposeGroupRuntimeInfoBuilder(
        val composeGroupKey: ComposeGroupKey,
        var isHashing: Boolean = false,
    ) {
        private val crc: CRC32 = CRC32()

        fun pushHashIfNecessary(value: Any?) {
            if (!isHashing) return
            if (value == null) return
            when (value) {
                is Boolean -> crc.update(if (value) 1 else 0)
                is String -> crc.update(value.toByteArray())
                is Int -> crc.update(value)
                is Byte -> crc.update(value.toInt())
                is Float -> crc.update(value.toRawBits())
                else -> crc.update(value.toString().toByteArray())
            }
        }

        fun createGroupRuntimeInfo(): ComposeGroupRuntimeInfo {
            return ComposeGroupRuntimeInfo(
                callSiteMethodFqn = callSiteMethodFqn,
                ComposeGroupInvalidationKey(crc.value.toInt())
            )
        }
    }

    override fun visitLdcInsn(value: Any?) {
        if (value is Int) lastIntValue = value
        stack.lastOrNull()?.pushHashIfNecessary(value)
        super.visitLdcInsn(value)
    }

    override fun visitMethodInsn(
        opcode: Int, owner: String?, name: String?, descriptor: String?, isInterface: Boolean
    ) {
        fun isMethod(ctMethod: CtMethod): Boolean {
            if (descriptor != ctMethod.methodInfo.descriptor) return false
            if (name != ctMethod.name) return false
            if (owner != ctMethod.declaringClass.name.replace(".", "/")) return false
            return true
        }

        if (isMethod(startRestartGroupMethod)) {
            stack.add(ComposeGroupRuntimeInfoBuilder(ComposeGroupKey(lastIntValue)))
        }

        if (isMethod(startReplaceGroupMethod)) {
            stack.add(ComposeGroupRuntimeInfoBuilder(ComposeGroupKey(lastIntValue)))
        }

        if (isMethod(endReplaceGroupMethod)) {
            val element = stack.removeLastOrNull() ?: return
            groups[element.composeGroupKey] = element.createGroupRuntimeInfo()
        }

        if (isMethod(endRestartGroupMethod)) {
            val element = stack.removeLastOrNull() ?: return
            groups[element.composeGroupKey] = element.createGroupRuntimeInfo()
        }

        if (isMethod(composerRememberedValueMethod)) {
            stack.lastOrNull()?.isHashing = true
        }

        if (isMethod(composerUpdateRememberedValueMethod)) {
            stack.lastOrNull()?.isHashing = false
        }

        stack.lastOrNull()?.apply {
            pushHashIfNecessary(owner)
            pushHashIfNecessary(name)
            pushHashIfNecessary(descriptor)
            pushHashIfNecessary(isInterface)
        }
    }

    override fun visitInvokeDynamicInsn(
        name: String?,
        descriptor: String?,
        bootstrapMethodHandle: Handle?,
        vararg bootstrapMethodArguments: Any?
    ) {
        stack.lastOrNull()?.apply {
            pushHashIfNecessary(name)
            pushHashIfNecessary(descriptor)
            pushHashIfNecessary(bootstrapMethodHandle?.name)
            pushHashIfNecessary(bootstrapMethodHandle?.owner)
            pushHashIfNecessary(bootstrapMethodHandle?.tag)
            pushHashIfNecessary(bootstrapMethodHandle?.desc)
            bootstrapMethodArguments.forEach { pushHashIfNecessary(it) }
        }
    }

    override fun visitInsn(opcode: Int) {
        stack.lastOrNull()?.pushHashIfNecessary(opcode)
        when (opcode) {
            Opcodes.ICONST_0 -> lastIntValue = 0
            Opcodes.ICONST_1 -> lastIntValue = 1
            Opcodes.ICONST_2 -> lastIntValue = 2
            Opcodes.ICONST_3 -> lastIntValue = 3
            Opcodes.ICONST_4 -> lastIntValue = 4
            Opcodes.ICONST_5 -> lastIntValue = 5
            Opcodes.ICONST_M1 -> lastIntValue = -1
        }
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        stack.lastOrNull()?.pushHashIfNecessary(opcode)
        stack.lastOrNull()?.pushHashIfNecessary(operand)
        lastIntValue = operand
    }

    override fun visitEnd() {
        if (stack.isNotEmpty()) {
            while (true) {
                val element = stack.removeLastOrNull() ?: break
                groups[element.composeGroupKey] = element.createGroupRuntimeInfo()
            }
        }
    }
}
