plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.sandroisu.releasely.smoke"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.sandroisu.releasely.smoke"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
        }
    }
}

