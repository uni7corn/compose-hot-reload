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

/* Make the current 'Hot Reload Version (aka version of this project) available */
run {
    val generatedSourceDir = file("build/generated/main/kotlin")

    tasks.register("writeVersionCode") {
        val file = generatedSourceDir.resolve("version.kt")
        val version = project.version.toString()

        inputs.property("version", version)
        outputs.file(file)

        doLast {
            file.parentFile.mkdirs()
            file.writeText(
                """
            package org.jetbrains.compose.reload
            
            internal const val HOT_RELOAD_VERSION = "$version"
        """.trimIndent()
            )
        }
    }

    kotlin {
        sourceSets.main.get().kotlin.srcDir(generatedSourceDir)
    }

    tasks.register("prepareKotlinIdeaImport") {
        dependsOn("writeVersionCode")
    }

    kotlin.target.compilations.getByName("main").compileTaskProvider.configure {
        dependsOn("writeVersionCode")
    }
}
