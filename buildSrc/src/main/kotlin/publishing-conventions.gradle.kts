/*
 * Copyright 2024-2025 JetBrains s.r.o. and Compose Hot Reload contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

plugins {
    `maven-publish` apply false
    signing
}


fun getSensitiveProperty(name: String): String? {
    return findProperty(name) as? String ?: System.getenv(name)
}

fun MavenPublication.signPublicationIfKeyPresent() {
    val keyId = getSensitiveProperty("libs.sign.key.id")
    val signingKey = getSensitiveProperty("libs.sign.key.private")
    val signingKeyPassphrase = getSensitiveProperty("libs.sign.passphrase")
    if (!signingKey.isNullOrBlank()) {
        extensions.configure<SigningExtension>("signing") {
            useInMemoryPgpKeys(keyId, signingKey, signingKeyPassphrase)
            sign(this@signPublicationIfKeyPresent)
        }
    }
}

plugins.withType<MavenPublishPlugin>().all {
    publishing {
        repositories {
            if (getSensitiveProperty("libs.sonatype.user") != null) {
                maven("https://oss.sonatype.org/service/local/staging/deploy/maven2/") {
                    name = "mavenCentral"
                    credentials {
                        username = getSensitiveProperty("libs.sonatype.user")
                        password = getSensitiveProperty("libs.sonatype.password")
                    }
                }
            }

            maven("https://repo.sellmair.io") {
                name = "sellmair"
                credentials {
                    username = providers.gradleProperty("repo.sellmair.user").orNull
                    password = providers.gradleProperty("repo.sellmair.password").orNull
                }
            }

            maven("https://packages.jetbrains.team/maven/p/firework/dev") {
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

        publications.withType<MavenPublication>().configureEach {
            this.signPublicationIfKeyPresent()
            pom {
                name = project.name
                description = "Compose Hot Reload implementation"
                url = "https://github.com/JetBrains/compose-hot-reload"

                licenses {
                    license {
                        name = "The Apache Software License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "repo"
                    }
                }

                developers {
                    developer {
                        id = "JetBrains"
                        name = "JetBrains Team"
                        organization = "JetBrains"
                        organizationUrl = "https://www.jetbrains.com"
                    }
                }

                scm {
                    url = "https://github.com/JetBrains/compose-hot-reload"
                }
            }
        }
    }
}
