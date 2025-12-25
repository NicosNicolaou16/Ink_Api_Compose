import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.nicos.ink_api_compose"
    buildToolsVersion = "36.0.0"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nicos.ink_api_compose"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget("21")
            freeCompilerArgs = listOf("-Xannotation-default-target=param-property")
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    // Architecture
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Graphic Core
    implementation(libs.androidx.graphics.core)
    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.hilt.compose)
    // Ink API
    implementation(libs.ink.authoriring)
    implementation(libs.ink.brush)
    implementation(libs.ink.geometry)
    implementation(libs.ink.nativeloader)
    implementation(libs.ink.rendering)
    implementation(libs.ink.strokes)
    implementation(libs.androidx.ink.storage)
    implementation(libs.androidx.ink.authoring.compose)
    // Motion Prediction
    implementation(libs.androidx.input.motionprediction)
    // Hilt
    implementation(libs.dagger.android)
    ksp(libs.dagger.compiler)
    ksp(libs.hilt.compiler)
    // Room Database
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)
    // Unit Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}