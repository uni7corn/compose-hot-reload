/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

@file:OptIn(ExperimentalComposeLibrary::class)

import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.reload.ComposeHotRun
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("org.jetbrains.compose.hot-reload")
    `maven-publish`
    `publishing-conventions`
}

kotlin {
    jvmToolchain(17)
}

tasks.create<ComposeHotRun>("runDev") {
    mainClass.set("org.jetbrains.compose.reload.jvm.tooling.RunKt")
    compilation.set(kotlin.target.compilations["dev"])
    systemProperty("orchestration.mode", "server")
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        this.jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

dependencies {
    implementation(compose.runtime)

    implementation(project(":hot-reload-core"))
    implementation(project(":hot-reload-orchestration"))

    implementation(compose.desktop.common)
    implementation(compose.material3)
    implementation(compose.components.resources)
    implementation(deps.coroutines.swing)
    implementation(deps.logback)
    implementation(deps.kotlinxDatetime)

    implementation(deps.evas)
    implementation(deps.evas.compose)

    testImplementation(kotlin("test"))
    testImplementation(deps.junit.jupiter)
    testImplementation(deps.junit.jupiter.engine)
    testImplementation(compose.uiTest)
    testImplementation(compose.desktop.currentOs)

    devCompileOnly(project(":hot-reload-agent"))

    /*
     Required to enforce no accidental resolve to the -api-jvm, published by the runtime api project.
     Usually such substitutions are made automatically by Gradle, but there seems to be an issue
     with the sub-publications of the KMP project
     */
    configurations.all {
        if (name == "composeHotReloadAgent") return@all
        if (name == "composeHotReloadDevTools") return@all

        resolutionStrategy.dependencySubstitution {
            substitute(module("org.jetbrains.compose.hot-reload:runtime-api-jvm"))
                .using(project(":hot-reload-runtime-api"))
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
