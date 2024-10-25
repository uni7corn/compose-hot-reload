@file:Suppress("UnstableApiUsage")

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}

val publishLocally by tasks.registering
subprojects {
    publishLocally.configure {
        this.dependsOn(tasks.named { name -> name == "publishAllPublicationsToLocalRepository" })
    }
}
