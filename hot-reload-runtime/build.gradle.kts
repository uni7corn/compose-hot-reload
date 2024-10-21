plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.library")
    `maven-publish`
    `publishing-conventions`
}

kotlin {
    compilerOptions {
        explicitApi()
    }

    jvmToolchain(17)
    jvm()

    androidTarget {
        publishLibraryVariants("release")
    }

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

    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
    }

    sourceSets.jvmMain.dependencies {
        implementation(deps.slf4j.api)
        implementation(compose.desktop.common)
        implementation(compose.material3)

        implementation(deps.javassist) // TODO: Separate production and dev build dependencies
        compileOnly(deps.hotswapAgentCore)
        compileOnly(project(":hot-reload-agent"))
    }

    sourceSets.jvmTest.dependencies {
        implementation(deps.junit.jupiter)
        implementation(deps.junit.jupiter.engine)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

android {
    compileSdk = 34
    namespace = "org.jetbrains.compose.reload"
}