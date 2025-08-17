// composeApp/build.gradle.kts
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        (f.inputStream().use { load(it) })
    }
}



val mapsKey: String = (
        localProps.getProperty("GOOGLE_MAPS_API_KEY")
            ?: project.findProperty("GOOGLE_MAPS_API_KEY") as String?
            ?: System.getenv("GOOGLE_MAPS_API_KEY")
            ?: ""
        )

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("com.google.gms.google-services")
}

repositories {
    google()
    mavenCentral()
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    sourceSets {
        val ktor = "2.3.12"

        val androidMain by getting {
            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.lottie.compose)
                implementation("io.insert-koin:koin-androidx-compose:3.5.6")

                implementation(libs.androidx.material.icons.extended)
                val gitlive = "2.2.0"
                implementation("dev.gitlive:firebase-common:$gitlive")
                implementation("dev.gitlive:firebase-auth:$gitlive")
                implementation("dev.gitlive:firebase-firestore:$gitlive")
                implementation(libs.coil.compose)
                implementation("androidx.activity:activity-compose:1.7.2")
                implementation("androidx.navigation:navigation-compose:2.7.0")
                implementation("com.google.accompanist:accompanist-permissions:0.32.0")

                implementation("com.google.android.gms:play-services-location:21.0.1")
                implementation("com.google.android.gms:play-services-maps:18.1.0")
                implementation("com.google.maps.android:maps-compose:2.11.3")

                // Ktor (needed because MainActivity references HttpClient via httpClient())
                implementation("io.ktor:ktor-client-core:$ktor")
                implementation("io.ktor:ktor-client-okhttp:$ktor")
                implementation("io.ktor:ktor-client-content-negotiation:$ktor")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor")
            }
        }

        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtimeCompose)

                // depends on the shared module
                implementation(projects.shared)
            }
        }

        val commonTest by getting {
            dependencies { implementation(libs.kotlin.test) }
        }
    }
}

android {
    namespace = "org.example.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.example.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        // Maps key from gradle.properties → AndroidManifest
        manifestPlaceholders["com.google.android.geo.API_KEY"] = mapsKey


    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }



    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.cloudinary.android)


    debugImplementation(compose.uiTooling)

}
