package org.jetbrains.compose.reload

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmRun
import org.jetbrains.kotlin.konan.file.File


internal fun Project.setupComposeHotReloadExecTasks() {
    kotlinJvmOrNull?.apply {
        target.createComposeHotReloadExecTask()
    }

    kotlinMultiplatformOrNull?.apply {
        targets.withType<KotlinJvmTarget>().all { target ->
            target.createComposeHotReloadExecTask()
        }
    }
}

private fun KotlinTarget.createComposeHotReloadExecTask() {
    @OptIn(InternalKotlinGradlePluginApi::class)
    project.tasks.register<KotlinJvmRun>("${name}Run") {
        configureJavaExecTaskForHotReload(project.provider { compilations.getByName("main") })
    }
}

internal fun JavaExec.configureJavaExecTaskForHotReload(compilation: Provider<KotlinCompilation<*>>) {
    group = "run"
    description = "Run the Compose application (Hot Reload enabled) | -PmainClass=..."

    if (project.composeHotReloadExtension.useJetBrainsRuntime.get()) {
        javaLauncher.set(project.serviceOf<JavaToolchainService>().launcherFor { spec ->
            @Suppress("UnstableApiUsage")
            spec.vendor.set(JvmVendorSpec.JETBRAINS)
            spec.languageVersion.set(JavaLanguageVersion.of(21))
        })
    }


    /* We do rely on the hotswap-agent */
    inputs.files(project.hotswapAgentConfiguration.files)

    setClasspath(project.files { compilation.get().createComposeHotReloadRunClasspath() })

    /* Generic JVM args for hot reload*/
    run {
        /* Will get us additional information at runtime */
        if (logger.isInfoEnabled) {
            jvmArgs("-Xlog:redefine+class*=info")
        }

        /* Setup the hotswap agent (using autoHotswap) */
        jvmArgs(
            "-javaagent:" +
                    "${project.hotswapAgentConfiguration.files.joinToString(File.pathSeparator)}=" +
                    "autoHotswap=true"
        )
    }

    /* JBR args */
    run {
        /* Non JBR JVMs will hate our previous JBR specific args */
        jvmArgs("-XX:+IgnoreUnrecognizedVMOptions")

        /* Enable DCEVM enhanced hotswap capabilities */
        jvmArgs("-XX:+AllowEnhancedClassRedefinition")

        /* We're providing the HotswapAgent by providing the har from 'hotswapAgentConfiguration' */
        jvmArgs("-XX:HotswapAgent=external")
    }

    /* Support for non jbr */
    if (!project.composeHotReloadExtension.useJetBrainsRuntime.get()) {

        /* Required for Hotswap Agent on non JBR */
        jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
        jvmArgs("--add-opens=java.base/java.ref=ALL-UNNAMED")
        jvmArgs("--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED")
        jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED")
        jvmArgs("--add-opens=java.desktop/com.sun.beans=ALL-UNNAMED")
        jvmArgs("--add-opens=java.desktop/com.sun.beans.introspect=ALL-UNNAMED")
        jvmArgs("--add-opens=java.desktop/com.sun.beans.util=ALL-UNNAMED")
    }

    /* Setup re-compiler */
    val compileTaskName = compilation.map { it.compileKotlinTaskName }

    systemProperty("compose.build.root", project.rootDir.absolutePath)
    systemProperty("compose.build.project", project.path)
    systemProperty("compose.build.compileTask", compileTaskName.get())

    doFirst {
        systemProperty("compose.build.compileTask", compileTaskName.get())
    }

    mainClass.value(
        project.providers.gradleProperty("mainClass")
            .orElse(project.providers.systemProperty("mainClass"))
    )

    doFirst {
        if (!mainClass.isPresent) {
            throw IllegalArgumentException(ErrorMessages.missingMainClassProperty())
        }

        logger.info("Running ${mainClass.get()}...")
        logger.info("Classpath:\n${classpath.joinToString("\n")}")
    }
}