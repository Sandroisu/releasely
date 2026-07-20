plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "io.github.sandroisu.fixture"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.sandroisu.fixture"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}
