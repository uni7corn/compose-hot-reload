import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.tooling.core.buildNumber
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory

/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun main() {
    ensureCleanWorkingDirectory()

    val latestDevVersion = fetchLatestVersionFromFireworkDevRepository()
    val buildNumber = latestDevVersion.buildNumber ?: error("Missing build number in version $latestDevVersion")
    val currentVersion = KotlinToolingVersion(readGradleProperties("version"))
    val currentBuildNumber = currentVersion.buildNumber ?: error("Missing build number in version $currentVersion")
    val newVersion = currentVersion.toString().replace(currentBuildNumber.toString(), buildNumber.toString())

        //latestDevVersion.toString().replace("-$buildNumber", "-${buildNumber + 1}")
    writeGradleProperties("version", newVersion)

    command("./gradlew", "updateVersions")

    command("git", "add", ".")
    command("git", "commit", "-m", "v$newVersion")
    command("git", "tag", "v$newVersion")
}


private fun fetchLatestVersionFromFireworkDevRepository(): KotlinToolingVersion {
    val mavenMetadataUrl =
        "https://packages.jetbrains.team/maven/p/firework/dev/org/jetbrains/compose/hot-reload/core/maven-metadata.xml"

    val mavenMetadata = URI(mavenMetadataUrl).toURL().openStream().use { inputStream ->
        inputStream.readAllBytes().decodeToString()
    }

    val rawVersion = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(mavenMetadata.byteInputStream())
        .getElementsByTagName("latest").item(0).textContent

    return KotlinToolingVersion(rawVersion)
}
