plugins {
    `maven-publish` apply false
}

plugins.withType<MavenPublishPlugin>().all {
    extensions.configure<PublishingExtension> {
        repositories {
            maven("https://repo.sellmair.io") {
                name = "sellmair"
                credentials {
                    username = providers.gradleProperty("repo.sellmair.user").orNull
                    password = providers.gradleProperty("repo.sellmair.password").orNull
                }
            }

            maven("https://packages.jetbrains.team/maven/p/firework/dev"){
                name = "firework"
                credentials {
                    username = providers.gradleProperty("spaceUsername").orNull
                    password = providers.gradleProperty("spacePassword").orNull
                }
            }

            maven(rootProject.layout.buildDirectory.dir("repo")) {
                name = "local"
            }
        }
    }
}
