package org.jetbrains.compose.reload.core

import java.util.zip.CRC32

@OptIn(ExperimentalStdlibApi::class)
public fun String.asFileName(): String {
    var result = this
    result = result.replace("""\\W+""", "_")

    if (result.length > 200) {
        val crc = CRC32()
        crc.update(result.toByteArray())
        val crcHex = crc.value.toInt().toHexString()

        result = result.take(100) + "..." + result.takeLast(100) + "-$crcHex"
    }

    return result
}
