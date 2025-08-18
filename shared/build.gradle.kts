// shared/build.gradle.kts
import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import java.util.Properties

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
    applyDefaultHierarchyTemplate()

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    val iosX64Target = iosX64()
    val iosArm64Target = iosArm64()
    val iosSimArm64Target = iosSimulatorArm64()

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
        // ↓ Pin GitLive to Kotlin/Native 2.1.x-compatible build to avoid ABI mismatch with your current toolchain
        val gitlive = "2.1.0" // was 2.2.0 (caused 'Incompatible ABI version' on iOS)

        // ---- common ----
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)

                // Firebase (GitLive)
                implementation("dev.gitlive:firebase-common:$gitlive")
                implementation("dev.gitlive:firebase-auth:$gitlive")
                implementation("dev.gitlive:firebase-firestore:$gitlive")
                implementation("io.insert-koin:koin-core:3.5.6")



                implementation("io.insert-koin:koin-core:3.5.6")

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
                implementation("io.insert-koin:koin-android:3.5.6") // for androidContext()
                implementation(libs.cloudinary.android)
                // Ktor Android engine
                implementation("io.ktor:ktor-client-okhttp:$ktor")
                // Android-only deps used in shared Android code
                implementation(libs.play.services.location)
                // REQUIRED for viewModelScope in androidMain actuals
                implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
                implementation("com.google.android.gms:play-services-location:21.3.0")
            }
        }

        // ---- iOS ----
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
    buildFeatures { buildConfig = true }
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()

        val props = Properties().apply {
            val file = rootProject.file("local.properties")
            if (file.exists()) file.inputStream().use { load(it) }
        }
        val googleMapsKey = props.getProperty("GOOGLE_MAPS_API_KEY") ?: ""
        val cloudName = props.getProperty("CLOUD_NAME") ?: ""
        val cloudApiKey = props.getProperty("CLOUD_API_KEY") ?: ""
        val cloudApiSecret = props.getProperty("CLOUD_API_SECRET") ?: ""

        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"$googleMapsKey\"")
        buildConfigField("String", "CLOUD_NAME", "\"$cloudName\"")
        buildConfigField("String", "CLOUD_API_KEY", "\"$cloudApiKey\"")
        buildConfigField("String", "CLOUD_API_SECRET", "\"$cloudApiSecret\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
