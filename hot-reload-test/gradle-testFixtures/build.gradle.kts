/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

plugins {
    kotlin("jvm")
    `maven-publish`
    `kotlin-conventions`
    `publishing-conventions`
    `tests-with-compiler`
    org.jetbrains.kotlinx.benchmark
}

kotlin {
    explicitApi()

    compilerOptions {
        optIn.add("org.jetbrains.compose.reload.test.core.InternalHotReloadTestApi")
    }

    target.compilations.create("benchmark") {
        associateWith(target.compilations.getByName("main"))
        defaultSourceSet {
            dependencies {
                implementation(kotlin("compiler-embeddable"))
                implementation(deps.benchmark.runtime)
                implementation(project.dependencies.testFixtures(project(":hot-reload-core")))
            }
        }
    }
}

benchmark {
    targets.register("benchmark")
}

tasks.named { it == "benchmarkBenchmark" }.configureEach {
    this as JavaExec
    testWithCompiler {
        tasks.add(this@configureEach)
    }
}

dependencies {
    implementation(project(":hot-reload-core"))
    implementation(project(":hot-reload-test:core"))
    implementation(project(":hot-reload-orchestration"))
    implementation(kotlin("reflect"))
    api(kotlin("test"))
    api(kotlin("tooling-core"))
    api(deps.junit.jupiter)
    api(deps.coroutines.test)
    implementation(deps.junit.jupiter.engine)
}

publishingConventions {
    artifactId = "hot-reload-gradle-testFixtures"
    oldArtifactId = "test-gradle"
}
