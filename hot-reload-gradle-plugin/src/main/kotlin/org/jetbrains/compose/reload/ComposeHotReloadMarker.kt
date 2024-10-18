package org.jetbrains.compose.reload

import org.gradle.api.attributes.Attribute

/**
 * See [setupComposeHotReloadVariant]
 */
internal enum class ComposeHotReloadMarker {
    Cold, Hot;

    companion object {
        val attribute = Attribute.of(ComposeHotReloadMarker::class.java)
    }
}

