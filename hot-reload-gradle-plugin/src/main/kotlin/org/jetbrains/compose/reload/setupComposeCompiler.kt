package org.jetbrains.compose.reload

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

fun Project.setupComposeCompiler() {
    /*
    TODO: What would be the best way to configure the compose compiler
     in this 'special', only when compiling for 'dev' runs
     */
    project.tasks.withType(KotlinJvmCompile::class.java).configureEach { compileTask ->
        compileTask.disableLambdaMemoizationIfPossible()
    }
}

private fun KotlinCompilationTask<*>.disableLambdaMemoizationIfPossible() {
    if (
        project.kotlinToolingVersion >= KotlinToolingVersion("2.1.20-dev") ||
        project.kotlinToolingVersion.classifier.orEmpty().contains("chr")
    ) {
        compilerOptions.freeCompilerArgs.addAll(
            "-P", "plugin:androidx.compose.compiler.plugins.kotlin:lambdaMemoization=false"
        )
    }
}