plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    `publishing-conventions`
}

gradlePlugin {
    plugins.create("hot-reload") {
        id = "org.jetbrains.compose-hot-reload"
        implementationClass = "org.jetbrains.compose.reload.ComposeHotReloadPlugin"
    }
}

dependencies {
    compileOnly(kotlin("gradle-plugin"))
}