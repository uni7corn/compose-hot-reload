plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    `publishing-conventions`
}

publishing {
    publications.create<MavenPublication>("plugin") {
        from(components["java"])
    }
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

    val writeBuildConfig = tasks.register("writeBuildConfig") {
        val file = generatedSourceDir.resolve("BuildConfig.kt")

        val version = project.version.toString()
        inputs.property("version", version)

        val hotswapAgentCore = deps.hotswapAgentCore.get().toString()
        inputs.property("hotswapAgentCore", hotswapAgentCore)

        outputs.file(file)

        doLast {
            file.parentFile.mkdirs()
            file.writeText(
                """
            package org.jetbrains.compose.reload
            
            internal const val HOT_RELOAD_VERSION = "$version"
            
            internal const val HOTSWAP_AGENT_CORE = "$hotswapAgentCore"
        """.trimIndent()
            )
        }
    }

    kotlin {
        sourceSets.main.get().kotlin.srcDir(generatedSourceDir)
    }

    tasks.register("prepareKotlinIdeaImport") {
        dependsOn(writeBuildConfig)
    }

    kotlin.target.compilations.getByName("main").compileTaskProvider.configure {
        dependsOn(writeBuildConfig)
    }
}
