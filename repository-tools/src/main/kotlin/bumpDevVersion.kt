import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.tooling.core.buildNumber

/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun main() {
    ensureCleanWorkingDirectory()

    val version = KotlinToolingVersion(readGradleProperties("version"))
    val buildNumber = version.buildNumber ?: error("Missing build number in version $version")
    val newVersion = version.toString().replace("-$buildNumber", "-${buildNumber + 1}")
    writeGradleProperties("version", newVersion)

    command("./gradlew", "updateVersions")

    command("git", "add", ".")
    command("git", "commit", "-m", "v$newVersion")
    command("git", "push")

    command("git", "tag", "v$newVersion")
    command("git", "push", "origin", "tag", "v$newVersion")
}
