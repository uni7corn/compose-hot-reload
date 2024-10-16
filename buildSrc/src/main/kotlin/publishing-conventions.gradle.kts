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
        }
    }
}
