plugins {
    kotlin("jvm")
    `maven-publish`
    `publishing-conventions`
    `tests-with-compiler`

}

dependencies {
    implementation(project(":hot-reload-core"))

    implementation(deps.slf4j.api)
    implementation(deps.asm)
    implementation(deps.asm.tree)

    testImplementation(deps.logback)
}

publishing {
    publications.create("maven", MavenPublication::class) {
        from(components["java"])
    }
}