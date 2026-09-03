plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "moe.evil.hyperstatus.xposed"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 36
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        disable += "UseKtx"
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(projects.shared)
    compileOnly(projects.xbinterface)
    compileOnly(libs.libxposed.api)
    implementation(libs.kavaref.core)
    implementation(libs.kavaref.extension)
    implementation(libs.kavaref.android)
    debugImplementation(libs.clikt.core)
    implementation(libs.hiddenapibypass)
}
