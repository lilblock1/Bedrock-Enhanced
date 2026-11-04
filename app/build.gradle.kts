plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.bedrockenhanced.overlay"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bedrockenhanced.overlay"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.1"
    }
}
