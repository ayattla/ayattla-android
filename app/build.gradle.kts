plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ayattla.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ayattla.app"
        minSdk = 26
        targetSdk = 36
        versionCode = (System.getenv("AYT_VERSION_CODE") ?: "2").toInt()
        versionName = System.getenv("AYT_VERSION_NAME") ?: "2.0.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("AYT_KS") ?: "/root/.ayattla-android/upload.jks")
            storePassword = System.getenv("AYT_KS_PASS")
            keyAlias = "upload"
            keyPassword = System.getenv("AYT_KS_PASS")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (!System.getenv("AYT_KS_PASS").isNullOrEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    androidResources { noCompress += "sqlite" }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all { it.testLogging { events("passed", "failed"); showStandardStreams = false } }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.transformer)
    implementation(libs.media3.effect)
    implementation(libs.glance.appwidget)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.json)
}
