import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins.withType<KotlinPluginWrapper> {
    extensions.configure<KotlinProjectExtension> {
        jvmToolchain {
            this.languageVersion = JavaLanguageVersion.of(21)
            this.vendor = JvmVendorSpec.JETBRAINS
        }
    }
}

plugins.withType<KotlinMultiplatformPluginWrapper> {
    extensions.configure<KotlinProjectExtension> {
        jvmToolchain {
            this.languageVersion = JavaLanguageVersion.of(21)
            this.vendor = JvmVendorSpec.JETBRAINS
        }
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        this.jvmTarget.set(JvmTarget.JVM_11)
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}
