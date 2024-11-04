package org.jetbrains.compose.reload.utils

import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.Screenshot
import java.awt.Color
import javax.imageio.ImageIO
import kotlin.io.path.*
import kotlin.math.absoluteValue
import kotlin.test.fail

suspend fun HotReloadTestFixture.checkScreenshot(name: String) {
    orchestration.sendMessage(OrchestrationMessage.TakeScreenshotRequest())
    val screenshot = skipToMessage<Screenshot>()

    val directory = Path("src/functionalTest/resources/screenshots")
        .resolve(testClassName.asFileName().replace(".", "/"))
        .resolve(testMethodName.asFileName())

    val screenshotName = "$name.${screenshot.format}"
    val expectFile = directory.resolve(screenshotName)

    if (!expectFile.exists()) {
        expectFile.createParentDirectories()
        expectFile.writeBytes(screenshot.data)
        fail("Screenshot '$expectFile' did not exist; Generated")
    }

    val imageDiff = describeDifference(expectFile.readBytes(), screenshot.data)

    if (imageDiff.isNotEmpty()) {
        val actualFile = expectFile.resolveSibling("${expectFile.nameWithoutExtension}-actual.${screenshot.format}")
        actualFile.writeBytes(screenshot.data)
        fail("Screenshot ${expectFile.toUri()} does not match\n" + imageDiff.joinToString("\n"))
    }
}

private fun String.asFileName(): String {
    return replace("""\\W+""", "_")
}

/**
 * @param expected The binary representation of the expected image
 * @param actual The binary representation of the actual image
 * @param backgroundColor Pixels with background color will not be incorporated in calculating the 'diff value'.
 * @param maxDiffValue The threshold of 'diff' value from which the images are to be considered 'non-equal': The diff
 * value is a number between 0 and 1, describing how different the images are. 0 means that the images are absolutely
 * identical. 1.0 would mean the complete opposite (every black pixel would be white and every white pixel would be black)
 *
 * @return The differences between the images in human-readable form, or an empty list if the images are
 * equal (enough)
 */
private fun describeDifference(
    expected: ByteArray, actual: ByteArray,
    backgroundColor: Color = Color.WHITE,
    maxDiffValue: Float = 0.01f
): List<String> {
    val expectedImage = ImageIO.read(expected.inputStream())
    val actualImage = ImageIO.read(actual.inputStream())
    val diffs = mutableListOf<String>()


    if (expectedImage.width != actualImage.width) {
        diffs.add("Expected width '${expectedImage.width}', found '${actualImage.width}'")
    }

    if (expectedImage.height != actualImage.height) {
        diffs.add("Expected height '${expectedImage.height}', found '${actualImage.height}'")
    }

    /* Return early if dimensions do not match */
    if (diffs.isNotEmpty()) return diffs

    var diff = 0
    var countingPixels = 0
    for (x in 0 until expectedImage.width) {
        for (y in 0 until expectedImage.height) {
            val expectedColor = Color(expectedImage.getRGB(x, y))
            val actualColor = Color(actualImage.getRGB(x, y))

            if (expectedColor != backgroundColor || actualColor != backgroundColor) {
                countingPixels++
            }

            diff += (expectedColor.red - actualColor.red).absoluteValue
            diff += (expectedColor.green - actualColor.green).absoluteValue
            diff += (expectedColor.blue - actualColor.blue).absoluteValue
        }
    }

    val diffFraction = diff / (countingPixels * 256 * 3).toFloat()
    if (diffFraction > maxDiffValue) {
        diffs.add("Image diff value is ${diffFraction.toString().take(4)}")
    }

    return diffs
}

