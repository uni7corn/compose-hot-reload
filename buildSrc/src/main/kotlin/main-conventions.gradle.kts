import gradle.kotlin.dsl.accessors._806313a808f14b6ebc1c25125e29f65e.versionCatalogs
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