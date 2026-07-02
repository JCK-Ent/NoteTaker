import java.net.HttpURLConnection
import java.net.URL

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.jckent.notetaker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jckent.notetaker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
    packaging {
        jniLibs {
            pickFirsts += setOf("lib/arm64-v8a/*.so", "lib/x86_64/*.so")
        }
    }
}

// Download the Vosk small English model at build time and bundle it into the APK assets.
// Gradle skips this task when the zip already exists, so it only downloads once.
tasks.register("downloadVoskModel") {
    val assetZip = project.file("src/main/assets/vosk-model-small-en-us-0.15.zip")
    outputs.file(assetZip)

    doLast {
        if (assetZip.exists()) return@doLast
        println("Downloading Vosk English model (~40 MB) — bundled once into the APK…")
        assetZip.parentFile.mkdirs()
        val conn = URL(
            "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
        ).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout   = 300_000
        try {
            conn.inputStream.use { it.copyTo(assetZip.outputStream()) }
        } catch (e: Exception) {
            assetZip.delete()
            throw e
        } finally {
            conn.disconnect()
        }
        println("Vosk model downloaded and added to assets.")
    }
}
tasks.named("preBuild") { dependsOn("downloadVoskModel") }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.vosk.android)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
