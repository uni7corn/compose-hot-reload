plugins {
    kotlin("jvm")
    `maven-publish`
    `publishing-conventions`
    `java-test-fixtures`
}

kotlin {
    compilerOptions {
        explicitApi()
    }
}

dependencies {
    testFixturesImplementation(kotlin("tooling-core"))
    testFixturesImplementation(deps.junit.jupiter)
}