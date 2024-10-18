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
    id("org.jetbrains.compose-hot-reload") version "1.0.0-dev.14" // <- add this additionally
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

### Optional: Create a custom entry point to launch your hot application
```kotlin
// build.gradle.kts
tasks.register<ComposeHotRun>("runHot") {
    mainClass.set("my.app.MainKt")
}
```

#### 💡The JBR can also be downloaded automatically by Gradle (foojay)
https://github.com/gradle/foojay-toolchains
```kotlin
// settings.gradle.kts
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
```

### Provide an Entry Point for your UI to hot-reload
```kotlin
@Composable 
fun App() {
    HotReload {
        MainPage()
    }
}
```

```kotlin
@Composable 
fun App() {
    linkUi("com.example.MainKt", "MainPage") // <- FQN of the function you want to hot reload 
}
```