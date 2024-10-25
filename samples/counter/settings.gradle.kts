pluginManagement {
    plugins {
        id("org.jetbrains.compose-hot-reload") version "1.0.0-dev.24"
    }

    repositories {
        maven("https://repo.sellmair.io")
        maven(file("../..//build/repo"))
        mavenCentral()
    }

    plugins {
        kotlin("multiplatform") version "2.0.21"
        kotlin("plugin.compose") version "2.0.21"
        id("org.jetbrains.compose") version "1.7.0"
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://repo.sellmair.io")
        maven(file("../..//build/repo"))
        mavenCentral()
        google()
    }
}

include(":app")
include(":widgets")