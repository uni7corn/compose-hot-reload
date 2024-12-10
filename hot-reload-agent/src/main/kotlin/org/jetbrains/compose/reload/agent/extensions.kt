package org.jetbrains.compose.reload.agent

import java.util.ServiceLoader

interface ComposeReloadPremainExtension {
    /**
     * Called as last step of the 'Agents' premain method.
     */
    fun premain()

    companion object {
        fun load(): Sequence<ComposeReloadPremainExtension> {
            return ServiceLoader.load(
                ComposeReloadPremainExtension::class.java, ClassLoader.getSystemClassLoader()
            ).asSequence()
        }
    }
}
