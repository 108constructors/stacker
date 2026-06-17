plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.shawkang.stackerassist"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shawkang.stackerassist"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "2.0"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
