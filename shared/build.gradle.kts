// shared/build.gradle.kts
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("co.touchlab.skie") version "0.10.1"
    alias(libs.plugins.kotlinxSerialization)
}

repositories {
    google()
    mavenCentral()
}

kotlin {
    // New source-set hierarchy (replaces deprecated ios())
    applyDefaultHierarchyTemplate()

    // Android target
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    // iOS targets (no CocoaPods)
    val iosX64Target = iosX64()
    val iosArm64Target = iosArm64()
    val iosSimArm64Target = iosSimulatorArm64()

    // Single XCFramework output named "Shared"
    val xcf = XCFramework("Shared")
    listOf(iosX64Target, iosArm64Target, iosSimArm64Target).forEach { t ->
        t.binaries.framework {
            baseName = "Shared"
            isStatic = true
            xcf.add(this)
        }
    }

    sourceSets {
        // ---- versions ----
        val ktor = "2.3.12"
        val kotlinxJson = "1.6.0"

        // ---- common ----
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)

                // Firebase (GitLive) via your version catalog BOM
                implementation(project.dependencies.platform(libs.firebase.bom))
                implementation(libs.firebase.firestore)
                implementation(libs.firebase.common)
                implementation(libs.firebase.auth)

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxJson")

                // Ktor core + JSON
                implementation("io.ktor:ktor-client-core:$ktor")
                implementation("io.ktor:ktor-client-content-negotiation:$ktor")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor")
            }
        }
        val commonTest by getting {
            dependencies { implementation(libs.kotlin.test) }
        }

        // ---- Android ----
        val androidMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.android)

                // Ktor Android engine
                implementation("io.ktor:ktor-client-okhttp:$ktor")

                // Android-only deps used in shared Android code
                implementation(libs.play.services.location)

                // REQUIRED for viewModelScope in androidMain actuals
                implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
            }
        }

        // ---- iOS (aggregate iosMain exists via applyDefaultHierarchyTemplate) ----
        val iosMain by getting {
            dependencies {
                // Ktor iOS engine (no CocoaPods)
                implementation("io.ktor:ktor-client-darwin:$ktor")
            }
        }
    }
}

android {
    namespace = "org.example.project.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
