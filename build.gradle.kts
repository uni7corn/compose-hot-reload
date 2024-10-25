@file:Suppress("UnstableApiUsage")

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}

val publishLocally by tasks.registering {
    /*  Update version used in samples */
    val sampleSettings = fileTree("samples") {
        include("**/settings.gradle.kts")
    }.files

    val version = project.version.toString()
    inputs.property("version", version)

    doLast {
        logger.quiet("Found: $sampleSettings")
        sampleSettings.forEach { settingsFile ->
            val text = settingsFile.readText()
            val declaration = """id("org.jetbrains.compose-hot-reload") version"""
            val newText = text.lines().map { line ->
                if (declaration !in line) return@map line
                val indent = line.substringBefore(declaration)
                indent + declaration + " \"${version}\""
            }.joinToString("\n")
            settingsFile.writeText(newText)

        }
    }
}

subprojects {
    publishLocally.configure {
        this.dependsOn(tasks.named { name -> name == "publishAllPublicationsToLocalRepository" })
    }
}
