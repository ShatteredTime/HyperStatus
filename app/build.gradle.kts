plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "moe.evil.hyperstatus"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "moe.evil.hyperstatus"
        minSdk = 36
        targetSdk = 37
        versionCode = 4
        versionName = "1.0.4"

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            optimization {
                enable = true
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    androidResources.additionalParameters.addAll(
        listOf("--allow-reserved-package-id", "--package-id", "0x64")
    )

    packaging {
        resources { merges += "META-INF/xposed/*" }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    runtimeOnly(projects.xposed)
    implementation(projects.shared)
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.preference)
}
