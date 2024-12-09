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
    api(deps.slf4j.api)

    testFixturesImplementation(kotlin("tooling-core"))
    testFixturesImplementation(deps.junit.jupiter)
    testFixturesCompileOnly(kotlin("compiler-embeddable"))
}

publishing {
    publications.create("maven", MavenPublication::class) {
        from(components["java"])
    }
}