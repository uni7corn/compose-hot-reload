package org.jetbrains.compose.reload.utils

import org.gradle.internal.impldep.org.junit.platform.commons.support.AnnotationSupport
import org.jetbrains.compose.reload.HOT_RELOAD_VERSION
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File
import kotlin.io.path.createFile
import kotlin.io.path.writeText

@ExtendWith(DefaultSettingsGradleKtsExtension::class)
annotation class DefaultSettingsGradleKts

internal class DefaultSettingsGradleKtsExtension : BeforeTestExecutionCallback {

    override fun beforeTestExecution(context: ExtensionContext) {
        if (!AnnotationSupport.isAnnotated(context.requiredTestMethod, DefaultSettingsGradleKts::class.java)) {
            return
        }

        val projectDir = context.getHotReloadTestFixtureOrThrow().projectDir
        val androidVersion = context.androidVersion
        projectDir.settingsGradleKts.createFile()
        projectDir.settingsGradleKts.writeText(
          createDefaultSettingsGradleKtsContent(
              kotlinVersion = context.kotlinVersion,
              composeVersion = context.composeVersion,
              androidVersion = androidVersion,
          )
        )
    }
}

fun createDefaultSettingsGradleKtsContent(
    kotlinVersion: TestedKotlinVersion? = TestedKotlinVersion.entries.last(),
    composeVersion: TestedComposeVersion? = TestedComposeVersion.entries.last(),
    androidVersion: TestedAndroidVersion? = null,
): String = """
    pluginManagement {
        plugins {
            kotlin("multiplatform") version "$kotlinVersion"
            kotlin("jvm") version "$kotlinVersion"
            kotlin("plugin.compose") version "$kotlinVersion"
            id("org.jetbrains.compose") version "$composeVersion"
            id("org.jetbrains.compose-hot-reload") version "$HOT_RELOAD_VERSION"
            ${androidVersion?.let { "id(\"com.android.application\") version \"$it\"" }}
        }
        
        repositories {
            maven(file("${localTestRepoDirectory.absolutePath.replace("\\", "\\\\")}"))
            mavenCentral()
            maven("https://packages.jetbrains.team/maven/p/firework/dev")
            google()
        }
    }
    
    dependencyResolutionManagement {
        repositories {
            maven(file("${localTestRepoDirectory.absolutePath.replace("\\", "\\\\")}"))
            mavenCentral()
            maven("https://packages.jetbrains.team/maven/p/firework/dev")
            google()
        }
    }
   """.trimIndent()

private val localTestRepoDirectory: File
    get() {
        val localRepoPath = System.getProperty("local.test.repo") ?: error("Missing 'local.test.repo' property")
        val localRepo = File(localRepoPath)
        if (!localRepo.exists()) error("Local repository does not exist: $localRepo")
        return localRepo
    }
