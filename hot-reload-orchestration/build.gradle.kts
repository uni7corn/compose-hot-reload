/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

plugins {
    kotlin("jvm")
    `maven-publish`
    `publishing-conventions`
    `java-test-fixtures`
    `api-validation-conventions`
}

kotlin {
    compilerOptions {
        explicitApi()
    }
}

dependencies {
    implementation(project(":hot-reload-core"))

    compileOnly(deps.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(deps.junit.jupiter)
    testImplementation(deps.junit.jupiter.engine)
    testImplementation(deps.coroutines.test)
    testImplementation(deps.asm)
    testImplementation(deps.asm.tree)
    testImplementation(project(":hot-reload-analysis"))
    testImplementation(kotlin ("reflect"))
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}
