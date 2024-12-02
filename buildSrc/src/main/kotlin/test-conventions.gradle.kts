@file:Suppress("UnstableApiUsage")

import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

plugins {
    `java-base` apply false
}

tasks.withType<AbstractTestTask>().configureEach {
    if (this is Test) {
        useJUnitPlatform()

        maxHeapSize = "4G"
        properties.filter { (key, _) -> key.startsWith("chr") }.forEach { (key, value) ->
            systemProperty(key, value.toString())
        }

        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
            vendor.set(JvmVendorSpec.JETBRAINS)
        })

        systemProperty(
            "firework.version", project.versionCatalogs.named("deps").findVersion("firework").get().requiredVersion
        )

    }

    testLogging {
        showStandardStreams = true
        events = setOf(TestLogEvent.STARTED, TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
    }
}

plugins.withType<KotlinPluginWrapper> {
    dependencies {
        if (project.name != "hot-reload-core") {
            "testImplementation"(testFixtures(project(":hot-reload-core")))
        }
    }
}