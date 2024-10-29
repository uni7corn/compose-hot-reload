pluginManagement {
    plugins {
        id("org.jetbrains.compose-hot-reload") version "1.0.0-dev.26"
    }

    repositories {
        maven(file("../..//build/repo"))
        maven("https://repo.sellmair.io")
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
        maven(file("../..//build/repo"))
        maven("https://repo.sellmair.io")
        mavenCentral()
        google()
    }
}

include(":app")
include(":widgets")