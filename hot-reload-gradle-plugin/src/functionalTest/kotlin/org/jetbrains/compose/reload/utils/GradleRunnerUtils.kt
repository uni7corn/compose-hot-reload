package org.jetbrains.compose.reload.utils

import org.gradle.testkit.runner.GradleRunner
import kotlin.io.path.Path

fun GradleRunner.addedArguments(vararg args: String): GradleRunner {
    return withArguments(this.arguments + args)
}

fun GradleRunner.withProjectTestKitDir(): GradleRunner {
    return withTestKitDir(Path("build/gradle-test-kit").toAbsolutePath().toFile())
}
