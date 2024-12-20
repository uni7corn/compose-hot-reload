@file:OptIn(ExperimentalComposeLibrary::class)

import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    `maven-publish`
    `publishing-conventions`
    `dev-runtime-jar`
}

kotlin {
    compilerOptions {
        explicitApi()
    }
}

dependencies {
    sharedImplementation(compose.runtime)

    devCompileOnly(project(":hot-reload-core"))
    devCompileOnly(project(":hot-reload-agent"))
    devCompileOnly(project(":hot-reload-runtime-api"))
    devCompileOnly(project(":hot-reload-orchestration"))
    devCompileOnly(compose.desktop.common)

    testImplementation(kotlin("test"))
    testImplementation(deps.junit.jupiter)
    testImplementation(deps.junit.jupiter.engine)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

publishing {
    publications.create("maven", MavenPublication::class) {
        from(components["java"])
    }
}
