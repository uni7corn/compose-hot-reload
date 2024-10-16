plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    `maven-publish`
    `publishing-conventions`
}

kotlin {
    compilerOptions {
        explicitApi()
    }

    jvm()

    macosArm64()
    macosX64()

    linuxX64()
    linuxArm64()

    iosSimulatorArm64()
    iosArm64()
    iosX64()

    sourceSets.commonMain.dependencies {
        implementation(compose.runtime)
        implementation(deps.coroutines.core)
    }

    sourceSets.jvmMain.dependencies {
        implementation(deps.slf4j.api)
        implementation(compose.desktop.common)
        implementation(compose.material3)
    }
}