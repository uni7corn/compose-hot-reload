@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
    kotlin("multiplatform") apply false
}

plugins.withType<KotlinMultiplatformPluginWrapper>().all {
    extensions.configure<KotlinMultiplatformExtension> {
        jvmToolchain(21)

        targets.withType<KotlinJvmTarget>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_1_8)
            }
        }
    }
}

/* Tests */
tasks.withType<AbstractTestTask>().configureEach {
    outputs.upToDateWhen { false }
    if (this is Test) {
        maxHeapSize = "4G"
    }
    testLogging {
        showStandardStreams = true
        events = setOf(TestLogEvent.STARTED, TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
    }
}