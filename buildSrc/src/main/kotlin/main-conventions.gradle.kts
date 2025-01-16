import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

plugins {
    `java-base` apply false
}

plugins.withType<KotlinPluginWrapper> {
    dependencies {
        if (project.name != "hot-reload-core") {
            "testImplementation"(testFixtures(project(":hot-reload-core")))
        }
    }
}
