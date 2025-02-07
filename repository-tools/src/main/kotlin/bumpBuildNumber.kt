import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.tooling.core.buildNumber
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun main() {
    ensureCleanWorkingDirectory()

    val gradlePropertiesFile = Path("gradle.properties")
    val gradlePropertiesText = gradlePropertiesFile.readText()
    val versionDeclarationRegex = Regex("""^version=(?<version>.*)""", RegexOption.MULTILINE)
    val versionDeclarationMatch = versionDeclarationRegex.find(gradlePropertiesText)
        ?: error("Cannot find 'version' in gradle.properties")
    val version = KotlinToolingVersion(versionDeclarationMatch.groups["version"]!!.value)
    val buildNumber = version.buildNumber ?: error("Missing build number in version $version")
    val newVersion = version.toString().replace("-$buildNumber", "-${buildNumber + 1}")
    val newGradlePropertiesText = gradlePropertiesText.replace(versionDeclarationRegex, "version=$newVersion")
    gradlePropertiesFile.writeText(newGradlePropertiesText)

    // Execute 'updateVersions' task
    ProcessBuilder("./gradlew", "updateVersions")
        .inheritIO().start().waitFor()

    ProcessBuilder("git", "add", ".").inheritIO().start().waitFor()
    ProcessBuilder("git", "commit", "-m", "v$newVersion").inheritIO().start().waitFor()
}
