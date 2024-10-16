pluginManagement {
    includeBuild("../..")

    plugins {
        kotlin("multiplatform") version "2.0.21"
        kotlin("plugin.compose") version "2.0.21"
        id("org.jetbrains.compose") version "1.6.11"
    }
}

includeBuild("../..")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

include(":app")
include(":widgets")