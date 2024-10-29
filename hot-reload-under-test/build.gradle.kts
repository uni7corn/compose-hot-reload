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
    compileOnly(project(":hot-reload-agent"))
    compileOnly(project(":hot-reload-orchestration"))
    compileOnly(project(":hot-reload-runtime-api"))
    compileOnly(project(":hot-reload-runtime-jvm"))

    implementation(deps.logback)
    implementation(deps.coroutines.swing)
    implementation(compose.uiTest)

    api(compose.desktop.common)
    api(compose.material3)
}