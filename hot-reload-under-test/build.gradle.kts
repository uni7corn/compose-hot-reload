@file:OptIn(ExperimentalComposeLibrary::class)

import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    `maven-publish`
    `publishing-conventions`
}

publishing {
    publications.register<MavenPublication>("maven") {
        from(components["java"])
    }
}

dependencies {
    implementation(project(":hot-reload-runtime-api"))
    implementation(project(":hot-reload-orchestration"))
    implementation(deps.logback)
    implementation(deps.coroutines.swing)
    implementation(compose.uiTest)

    api(compose.desktop.common)
    api(compose.material3)
}