@file:Suppress("NullableBooleanElvis")

package org.jetbrains.compose.reload.core.testFixtures

object TestEnvironment {
    val updateTestData = System.getProperty("chr.updateTestData")?.toBoolean()
        ?: System.getenv("CHR_UPDATE_TEST_DATA")?.toBoolean()
        ?: false

    val testOnlyLatestVersions = System.getProperty("chr.testOnlyLatestVersions")?.toBoolean()
        ?: System.getenv("CHR_TEST_ONLY_LATEST_VERSIONS")?.toBoolean()
        ?: System.getenv("TEST_ONLY_LATEST_VERSIONS")?.toBoolean()
        ?: false

    val fireworkVersion: String? = System.getProperty("firework.version")
}