plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "moe.evil.hyperstatus.shared"
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
}

kotlin {
    jvmToolchain(21)
}
