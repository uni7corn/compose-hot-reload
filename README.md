# 🔥 Compose Hot Reload Experiments

## Intro
This repository contains recent experiments for Hot Reloading Compose Applications.  
The intent is to upstream this repository into an official JetBrains product.

No guarantees apply. 

## State
The project publishes experimental builds 



### Add the 'sellmair' maven repository

(settings.gradle.kts)
```kotlin
pluginManagement {
    repositories {
        maven("https://repo.sellmair.io")
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://repo.sellmair.io")
    }
}

```

### Apply the Gradle plugin to your project

```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("org.jetbrains.compose-hot-reload") version "1.0.0-dev.2" // <- add this additionally
}
```

### Enable JetBrains Runtime (might require you to download JBR)
https://github.com/JetBrains/JetBrainsRuntime

// build.gradle.kts
```kotlin
composeHotReload {
    useJetBrainsRuntime = true
}
```