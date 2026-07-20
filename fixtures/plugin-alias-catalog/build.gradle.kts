plugins {
    // Applied in the subprojects; declared here only to pin a common classloader version.
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
}
