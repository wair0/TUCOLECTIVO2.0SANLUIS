plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.transpuntano.transpuntano20"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.transpuntano.transpuntano20"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "2.0.0"
        buildConfigField("String", "HOME_URL", "\"https://cuandollega.smartmovepro.net/transpuntano\"")
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.androidx.activity)
}
