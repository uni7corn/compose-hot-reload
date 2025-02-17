import java.net.URI
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.createParentDirectories
import kotlin.io.path.outputStream

fun main() {
    val sdkZipFile = Path(".local/android-sdk.zip").createParentDirectories()
    val sdkDir = Path(".local/android-sdk").createParentDirectories()
    val sdkManager = sdkDir.resolve("cmdline-tools/bin/sdkmanager.bat")


    URI("https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip?hl=de").toURL()
        .openStream().use { input ->
            input.copyTo(sdkZipFile.outputStream())
        }


    ZipFile(sdkZipFile.toFile()).use {
        it.entries().asSequence().forEach { entry ->
            val target = sdkDir.resolve(entry.name)
            if (entry.isDirectory) {
                target.createDirectories()
                return@forEach
            }


            it.getInputStream(entry).use { input ->
                target
                    .createParentDirectories()
                    .outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    sdkManager.toFile().setExecutable(true)

    val process = ProcessBuilder(
        "cmd", "/c", sdkManager.absolutePathString(),
        "--sdk_root=${sdkDir.absolutePathString()}",
        "--install",
        "platforms;android-34",
        "platforms;android-35",
        "build-tools;34.0.0",
        "build-tools;35.0.0",
        "platform-tools"
    )
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
    .start()

    runCatching {
        process.outputStream.writer().use { writer ->
            while (true) {
                writer.appendLine("y")
            }
        }
    }
}
