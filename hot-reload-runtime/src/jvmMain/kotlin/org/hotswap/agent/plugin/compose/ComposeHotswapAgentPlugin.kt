@file:OptIn(FlowPreview::class)

package org.hotswap.agent.plugin.compose

import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.hotswap.agent.annotation.*
import org.hotswap.agent.javassist.CtClass
import org.hotswap.agent.util.PluginManagerInvoker
import org.jetbrains.compose.reload.createLogger
import org.jetbrains.compose.reload.enableComposeHotReloadMode
import java.net.URL
import kotlin.time.Duration.Companion.milliseconds

private val logger = createLogger()

@Plugin(name = "Compose", testedVersions = ["2.0.1"])
internal class ComposeHotswapAgentPlugin {

    @Suppress("unused")
    @OnClassFileEvent(classNameRegexp = ".*", events = [FileEvent.MODIFY, FileEvent.CREATE, FileEvent.DELETE])
    fun prepareReload(ctClass: CtClass, appClassLoader: ClassLoader, url: URL) {
        logger.trace("prepareReload: $ctClass, $appClassLoader, $url")
        _beforeReload.update { it + 1 }
    }

    @Suppress("unused")
    @OnClassLoadEvent(classNameRegexp = ".*", events = [LoadEvent.REDEFINE])
    fun onAnyReload() {
        _onReload.update { it + 1 }
    }

    companion object {
        private val _beforeReload = MutableStateFlow(0L)
        private val beforeReload = _beforeReload.asSharedFlow()
            .sample(256.milliseconds)


        private val _onReload = MutableStateFlow(0L)
        val onReload = _onReload.asSharedFlow()
            .debounce(128.milliseconds)

        @JvmStatic
        @Suppress("unused") // @Init does magic!
        @Init
        fun init(appClassLoader: ClassLoader?) {
            if (appClassLoader == null) return
            PluginManagerInvoker.callInitializePlugin(ComposeHotswapAgentPlugin::class.java, appClassLoader)
            enableComposeHotReloadMode(appClassLoader)
        }

        init {
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            scope.launch {
                beforeReload.collect {
                    logger.debug("before reload: $it")
                }
            }

            scope.launch {
                onReload.collect {
                    logger.debug("on reload: $it")
                    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
                    Recomposer.clearErrors()
                }
            }
        }
    }
}
