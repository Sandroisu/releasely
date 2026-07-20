plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "io.github.sandroisu.fixture.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }
}
