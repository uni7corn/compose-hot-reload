package org.jetbrains.compose.reload.analysis

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.Checksum

internal fun Checksum.updateInt(value: Int) {
    val buffer = ByteBuffer.allocate(4).putInt(value).array()
    update(buffer)
}

internal fun Checksum.updateFloat(value: Float) {
    val buffer = ByteBuffer.allocate(4).putFloat(value).array()
    update(buffer)
}

internal fun Checksum.updateLong(value: Long) {
    val buffer = ByteBuffer.allocate(8).putLong(value).array()
    update(buffer)
}

internal fun Checksum.updateDouble(value: Double) {
    val buffer = ByteBuffer.allocate(8).putDouble(value).array()
    update(buffer)
}