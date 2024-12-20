@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        maven("https://packages.jetbrains.team/maven/p/firework/dev") {
            mavenContent {
                includeGroupByRegex("org.jetbrains.kotlin.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
/*
Configure Repositories / Dependencies
*/
dependencyResolutionManagement {
    versionCatalogs {
        create("deps") {
            from(files("dependencies.toml"))
        }
    }

    repositories {
        repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

        /* Getting firework artifacts for tests (such as test compiler) */
        maven("https://packages.jetbrains.team/maven/p/firework/dev") {
            mavenContent {
                includeVersionByRegex("org.jetbrains.*", ".*", ".*firework.*")
            }
        }

        google {
            mavenContent {
                includeGroupByRegex(".*android.*")
                includeGroupByRegex(".*androidx.*")
                includeGroupByRegex(".*google.*")
            }
        }

        mavenCentral()
    }
}

include(":hot-reload-agent")
include(":hot-reload-gradle-plugin")
include(":hot-reload-runtime-api")
include(":hot-reload-runtime-jvm")
include(":hot-reload-orchestration")
include(":hot-reload-under-test")

gradle.lifecycle.beforeProject {
    group = "org.jetbrains.compose"
    version = project.providers.gradleProperty("version").get()
}
