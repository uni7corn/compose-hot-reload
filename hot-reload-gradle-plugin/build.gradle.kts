@file:Suppress("UnstableApiUsage")

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
    `publishing-conventions`
}

publishing {
    publications.create<MavenPublication>("plugin") {
        from(components["java"])
    }
}


/* Setup integration test */
run {
    val main = kotlin.target.compilations.getByName("main")
    val integrationTest = kotlin.target.compilations.create("integrationTest")
    integrationTest.associateWith(main)

    tasks.register<Test>("integrationTest") {
        testClassesDirs = integrationTest.output.classesDirs
        classpath = integrationTest.output.allOutputs + integrationTest.runtimeDependencyFiles
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    dependsOn(":hot-reload-runtime:publishAllPublicationsToLocalRepository")
    systemProperty("local.test.repo", rootProject.layout.buildDirectory.dir("repo").get().asFile.absolutePath)
}

gradlePlugin {
    plugins.create("hot-reload") {
        id = "org.jetbrains.compose-hot-reload"
        implementationClass = "org.jetbrains.compose.reload.ComposeHotReloadPlugin"
        testSourceSet(sourceSets.getByName("integrationTest"))
    }
}

dependencies {
    val integrationTestImplementation by configurations
    val integrationTestRuntimeOnly by configurations

    compileOnly(kotlin("gradle-plugin"))
    implementation(gradleApi())
    implementation(gradleKotlinDsl())

    integrationTestImplementation(gradleTestKit())
    integrationTestImplementation(kotlin("test-junit"))
    integrationTestImplementation(deps.junit.jupiter)


    testImplementation(kotlin("test"))
    testImplementation(deps.junit.jupiter)
    testImplementation(deps.junit.jupiter.engine)
    testImplementation(kotlin("gradle-plugin"))
    testImplementation("com.android.tools.build:gradle:8.6.1")
}


/* Make the current 'Hot Reload Version (aka version of this project) available */
run {
    val generatedSourceDir = file("build/generated/main/kotlin")

    val writeBuildConfig = tasks.register("writeBuildConfig") {
        val file = generatedSourceDir.resolve("BuildConfig.kt")

        val versionProperty = project.providers.gradleProperty("version")
        inputs.property("version", versionProperty)

        val hotswapAgentCore = deps.hotswapAgentCore.get().toString()
        inputs.property("hotswapAgentCore", hotswapAgentCore)

        outputs.file(file)

        doLast {
            file.parentFile.mkdirs()
            file.writeText(
                """
            package org.jetbrains.compose.reload
            
            internal const val HOT_RELOAD_VERSION = "${versionProperty.get()}"
            
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


