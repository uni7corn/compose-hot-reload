package org.jetbrains.compose.reload.jvm.tooling

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.reload.core.createLogger
import java.lang.invoke.MethodHandles

private val classLoader = MethodHandles.lookup().lookupClass().classLoader

private val logger = createLogger()

@Composable
internal fun ComposeLogo(modifier: Modifier) {
    var bitmap: ImageBitmap? by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {

            runCatching {
                bitmap = classLoader.getResource("img/compose-logo.png")!!.openStream()
                    .buffered().use { input -> @Suppress("DEPRECATION") loadImageBitmap(input) }
            }.onFailure {
                logger.error("Failed loading compose-logo", it)
            }
        }
    }

    bitmap?.let { bitmap ->
        Image(bitmap, "Compose Logo", modifier = modifier)
    }
}