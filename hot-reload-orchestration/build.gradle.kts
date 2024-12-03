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

    target.compilations.create("coroutines")
}

dependencies {
    implementation(deps.slf4j.api)
    compileOnly(deps.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(deps.junit.jupiter)
    testImplementation(deps.junit.jupiter.engine)
    testImplementation(deps.coroutines.test)
    testImplementation(deps.logback)
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}

publishing {
    publications.create("maven", MavenPublication::class) {
        from(components["java"])
    }
}
