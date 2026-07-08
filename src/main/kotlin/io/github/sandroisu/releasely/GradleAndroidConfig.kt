package io.github.sandroisu.releasely

import java.nio.file.Path

enum class AndroidPluginType {
    APPLICATION,
    LIBRARY,
    DYNAMIC_FEATURE,
    TEST,
    UNKNOWN_ANDROID
}

data class GradleAndroidConfig(
    val gradleFile: Path,
    val androidPluginType: AndroidPluginType?,
    val hasAndroidPlugin: Boolean,
    val applicationId: String?,
    val namespace: String?,
    val compileSdk: Int?,
    val minSdk: Int?,
    val targetSdk: Int?,
    val versionCode: Int?,
    val versionName: String?,
    val minifyEnabled: Boolean?,
    val shrinkResources: Boolean?
)
