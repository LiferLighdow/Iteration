plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "com.liferlighdow.iteration"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.liferlighdow.iteration"
        minSdk = 24
        targetSdk = 37
        versionCode = 55
        versionName = "4.5.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
    lint {
        disable += "NullSafeMutableLiveData"
        abortOnError = false
        checkReleaseBuilds = false
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Compose
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    implementation(libs.materialColorUtils)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.backdrop)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
