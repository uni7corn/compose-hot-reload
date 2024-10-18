package org.jetbrains.compose.reload.utils

import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

val NamedDomainObjectContainer<out KotlinCompilation<*>>.main get() = getByName("main")