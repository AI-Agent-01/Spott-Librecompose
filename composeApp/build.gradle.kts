import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // Only configure iOS targets on Mac hosts to avoid Kotlin/Native toolchain issues on Windows
    if (HostManager.hostIsMac) {
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "ComposeApp"
                isStatic = true
            }
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.material3.window.size)
            implementation(libs.material.icons.extended)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
            // Navigation 3.0
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
        }
        if (HostManager.hostIsMac) {
            iosMain.dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.material.icons.core)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)

            // MapLibre Compose
            implementation(libs.maplibre.compose)
        }
    }
}

android {
    namespace = "com.spott"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.spott"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Expose MapTiler API key to Android code
        // Read from local.properties first (for local dev), then gradle.properties (for CI)
        val localProperties = java.util.Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        val maptileKey = localProperties.getProperty("MAPTILER_API_KEY") 
            ?: providers.gradleProperty("MAPTILER_API_KEY").orNull 
            ?: ""
        buildConfigField("String", "MAPTILER_API_KEY", "\"$maptileKey\"")
    }
    buildFeatures {
        // Required on AGP 8+ to generate BuildConfig
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.androidx.compose.ui.tooling)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
