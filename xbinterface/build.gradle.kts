plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "moe.evil.hyperstatus.xbinterface"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 36
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
