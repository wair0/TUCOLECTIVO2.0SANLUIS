plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "2.0.21"
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
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.3")

    // Compose fijado explícitamente para evitar que la resolución del BOM
    // deje el classpath de Kotlin sin los artefactos de Compose.
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.runtime:runtime:1.7.6")
    implementation("androidx.compose.animation:animation-core:1.7.6")
    implementation("androidx.compose.foundation:foundation:1.7.6")
    implementation("androidx.compose.ui:ui:1.7.6")
    implementation("androidx.compose.ui:ui-graphics:1.7.6")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.6")
    implementation("androidx.compose.material3:material3:1.3.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.6")
}
