import org.gradle.internal.jvm.inspection.JvmVendor
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("org.jetbrains.compose-hot-reload")
}

kotlin {

    jvm()

    composeHotReload {
        useJetBrainsRuntime = true
    }

    sourceSets.commonMain.dependencies {
        implementation("io.sellmair:evas:1.1.0")
        implementation("io.sellmair:evas-compose:1.1.0")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
        implementation("ch.qos.logback:logback-classic:1.5.9")
        implementation(compose.desktop.currentOs)
        implementation(compose.foundation)
        implementation(compose.material3)

        implementation(project(":widgets"))
    }
}
