package org.jetbrains.compose.reload.core

import java.io.File
import java.lang.System.getProperty
import java.nio.file.Path
import kotlin.io.path.Path

public enum class HotReloadProperty(public val key: String) {
    OrchestrationPort("compose.reload.orchestration.port"),
    IsHeadless("compose.reload.headless"),
    HotClasspath("compose.reload.hotApplicationClasspath"),


    GradleJavaHome("org.gradle.java.home"),
    GradleBuildRoot("gradle.build.root"),
    GradleBuildProject("gradle.build.project"),
    GradleBuildTask("gradle.build.task"),
    GradleBuildContinuous("gradle.build.continuous"),

    DevToolsEnabled("compose.reload.devToolsEnabled"),
    DevToolsClasspath("compose.reload.devToolsClasspath"),
    ;

    override fun toString(): String {
        return key
    }

}

public object HotReloadEnvironment {
    public val orchestrationPort: Int? = systemInt(HotReloadProperty.OrchestrationPort)
    public val isHeadless: Boolean = systemBoolean(HotReloadProperty.IsHeadless, false)
    public val hotApplicationClasspath: List<Path>? = systemFiles(HotReloadProperty.HotClasspath)

    public val gradleJavaHome: Path? = system(HotReloadProperty.GradleJavaHome)?.let(::Path)
    public val gradleBuildRoot: String? = system(HotReloadProperty.GradleBuildRoot)
    public val gradleBuildProject: String? = system(HotReloadProperty.GradleBuildProject)
    public val gradleBuildTask: String? = system(HotReloadProperty.GradleBuildTask)
    public val gradleBuildContinuous: Boolean = systemBoolean(HotReloadProperty.GradleBuildContinuous, false)

    public val devToolsEnabled: Boolean = systemBoolean(HotReloadProperty.DevToolsEnabled, true)
    public val devToolsClasspath: List<Path>? = systemFiles(HotReloadProperty.DevToolsClasspath)
}

public fun system(property: HotReloadProperty): String? = getProperty(property.key)

public fun systemBoolean(property: HotReloadProperty, default: Boolean): Boolean =
    getProperty(property.key)?.toBooleanStrict() ?: default

public fun systemInt(property: HotReloadProperty): Int? =
    getProperty(property.key)?.toIntOrNull()

public fun systemFiles(property: HotReloadProperty): List<Path>? =
    getProperty(property.key)?.split(File.pathSeparator)?.map(::Path)
