package org.jetbrains.compose.reload

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.konan.file.File

@Suppress("unused")
class ComposeHotReloadPlugin : Plugin<Project> {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    override fun apply(target: Project) {
        val hotReload = target.extensions.create("hotReload", ComposeHotReloadExtension::class.java)

        target.plugins.withType<KotlinMultiplatformPluginWrapper>().configureEach {
            val kotlin = target.extensions.getByName("kotlin") as KotlinMultiplatformExtension

            kotlin.applyDefaultHierarchyTemplate {
                withSourceSetTree(KotlinSourceSetTree("UI"))
            }

            kotlin.targets.all {
                if (this is KotlinMetadataTarget) return@all
                val mainCompilation = compilations.maybeCreate("main")
                val uiCompilation = compilations.maybeCreate("UI")

                uiCompilation.createUIElementsConfigurations()
                uiCompilation.associateWith(mainCompilation)

                mainCompilation.defaultSourceSet.dependencies {
                    implementation("org.jetbrains.compose:hot-reload-runtime:$HOT_RELOAD_VERSION")
                }

                project.configurations.getByName(uiCompilation.apiConfigurationName).attributes {
                    attribute(COMPOSE_USAGE_ATTRIBUTE, ComposeUsage.Main)
                }

                mainCompilation.runtimeDependencyConfigurationName?.let { runtimeConfigurationName ->
                    project.configurations.getByName(runtimeConfigurationName).attributes {
                        attribute(COMPOSE_USAGE_ATTRIBUTE, ComposeUsage.Main)
                    }
                }

                project.configurations.getByName(apiElementsConfigurationName).attributes {
                    attribute(COMPOSE_USAGE_ATTRIBUTE, ComposeUsage.Main)
                }

                project.configurations.getByName(runtimeElementsConfigurationName).attributes {
                    attribute(COMPOSE_USAGE_ATTRIBUTE, ComposeUsage.Main)
                }

                project.configurations.getByName(uiCompilation.compileDependencyConfigurationName).attributes {
                    attribute(COMPOSE_USAGE_ATTRIBUTE, ComposeUsage.UI)
                }

                uiCompilation.runtimeDependencyConfigurationName?.let { runtimeConfigurationName ->
                    project.configurations.getByName(runtimeConfigurationName).attributes {
                        attribute(COMPOSE_USAGE_ATTRIBUTE, ComposeUsage.UI)
                    }
                }

                if (this is KotlinJvmTarget) {
                    project.tasks.withType<JavaExec>().configureEach {
                        dependsOn(uiCompilation.compileTaskProvider)

                        val runtimeConfiguration = project.configurations.getByName(
                            uiCompilation.runtimeDependencyConfigurationName
                                ?: uiCompilation.compileDependencyConfigurationName
                        )

                        val uiModuleDependencies = runtimeConfiguration.incoming.artifactView {
                            componentFilter { identifier -> identifier is ProjectComponentIdentifier }
                        }.files

                        val uiBinaryDependencies = runtimeConfiguration.incoming.artifactView {
                            componentFilter { identifier -> identifier !is ProjectComponentIdentifier }
                        }.files

                        val coldUIClasspath = project.files(uiBinaryDependencies)
                        val hotUIClasspath = project.files(uiCompilation.output.allOutputs, uiModuleDependencies)

                        dependsOn(coldUIClasspath)
                        dependsOn(hotUIClasspath)

                        systemProperty("compose.build.root", project.rootDir.absolutePath)
                        systemProperty("compose.build.project", project.path)
                        systemProperty("compose.build.compileTask", uiCompilation.compileKotlinTaskName)

                        doFirst {
                            systemProperty("compose.ui.class.path.cold", coldUIClasspath.joinToString(File.pathSeparator))
                            systemProperty("compose.ui.class.path.hot", hotUIClasspath.joinToString(File.pathSeparator))
                        }
                    }
                }
            }
        }
    }
}

