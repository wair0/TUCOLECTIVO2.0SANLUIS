plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}

android {
    namespace = "com.tucolectivo.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.tucolectivo.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 30
        versionName = "3.0.0-native"
    }
    signingConfigs {
        create("release") {
            storeFile = file("../keystore/transpuntano20-release.jks")
            storePassword = "android"
            keyAlias = "transpuntano20"
            keyPassword = "android"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.androidx.activity)

    // Jetpack Compose BOM: mantiene runtime, UI, foundation, animation y Material3 sincronizados.
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.animation:animation-core")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}