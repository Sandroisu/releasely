package io.github.sandroisu.releasely

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GradleAndroidConfigScannerTest {

    private val scanner = GradleAndroidConfigScanner()

    @Test
    fun extractsKotlinDslNamespace() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            android {
                namespace = "com.example.app"
            }
        """.trimIndent()
    ) { result ->
        assertEquals(1, result.scannedGradleFileCount)
        assertTrue(result.failedGradleFiles.isEmpty())
        assertEquals("com.example.app", result.configs.single().namespace)
    }

    @Test
    fun extractsKotlinDslCompileSdk() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            android {
                compileSdk = 35
            }
        """.trimIndent()
    ) { result ->
        assertEquals(35, result.configs.single().compileSdk)
    }

    @Test
    fun extractsKotlinDslDefaultConfigValues() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            android {
                defaultConfig {
                    applicationId = "com.example.app"
                    minSdk = 26
                    targetSdk = 35
                    versionCode = 123
                    versionName = "1.2.3"
                }
            }
        """.trimIndent()
    ) { result ->
        val config = result.configs.single()
        assertEquals("com.example.app", config.applicationId)
        assertEquals(26, config.minSdk)
        assertEquals(35, config.targetSdk)
        assertEquals(123, config.versionCode)
        assertEquals("1.2.3", config.versionName)
    }

    @Test
    fun extractsKotlinDslReleaseBuildTypeValues() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            android {
                buildTypes {
                    getByName("release") {
                        isMinifyEnabled = true
                        isShrinkResources = true
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        val config = result.configs.single()
        assertEquals(true, config.minifyEnabled)
        assertEquals(true, config.shrinkResources)
        assertEquals(true, config.releaseMinifyEnabled)
        assertEquals(true, config.releaseShrinkResources)
    }

    @Test
    fun extractsGroovyDslApplicationId() = withGradleFile(
        fileName = "build.gradle",
        content = """
            android {
                defaultConfig {
                    applicationId "com.example.app"
                }
            }
        """.trimIndent()
    ) { result ->
        assertEquals("com.example.app", result.configs.single().applicationId)
    }

    @Test
    fun extractsGroovyDslSdkValues() = withGradleFile(
        fileName = "build.gradle",
        content = """
            android {
                compileSdk 35
                defaultConfig {
                    minSdk 26
                    targetSdk 35
                }
            }
        """.trimIndent()
    ) { result ->
        val config = result.configs.single()
        assertEquals(35, config.compileSdk)
        assertEquals(26, config.minSdk)
        assertEquals(35, config.targetSdk)
    }

    @Test
    fun extractsGroovyDslVersionValues() = withGradleFile(
        fileName = "build.gradle",
        content = """
            android {
                defaultConfig {
                    versionCode 123
                    versionName "1.2.3"
                }
            }
        """.trimIndent()
    ) { result ->
        val config = result.configs.single()
        assertEquals(123, config.versionCode)
        assertEquals("1.2.3", config.versionName)
    }

    @Test
    fun extractsGroovyDslReleaseBuildTypeValues() = withGradleFile(
        fileName = "build.gradle",
        content = """
            android {
                buildTypes {
                    release {
                        minifyEnabled true
                        shrinkResources true
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        val config = result.configs.single()
        assertEquals(true, config.minifyEnabled)
        assertEquals(true, config.shrinkResources)
        assertEquals(true, config.releaseMinifyEnabled)
        assertEquals(true, config.releaseShrinkResources)
    }

    @Test
    fun extractsKotlinDslReleaseMinifyEnabledFalse() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            plugins {
                id("com.android.application")
            }
            android {
                buildTypes {
                    release {
                        isMinifyEnabled = false
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        assertEquals(false, result.configs.single().releaseMinifyEnabled)
    }

    @Test
    fun extractsKotlinDslReleaseShrinkResourcesTrue() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            plugins {
                id("com.android.application")
            }
            android {
                buildTypes {
                    release {
                        isShrinkResources = true
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        assertEquals(true, result.configs.single().releaseShrinkResources)
    }

    @Test
    fun extractsKotlinDslGetByNameReleaseMinifyEnabledFalse() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            plugins {
                id("com.android.application")
            }
            android {
                buildTypes {
                    getByName("release") {
                        isMinifyEnabled = false
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        assertEquals(false, result.configs.single().releaseMinifyEnabled)
    }

    @Test
    fun extractsGroovyReleaseMinifyEnabledFalse() = withGradleFile(
        fileName = "build.gradle",
        content = """
            plugins {
                id 'com.android.application'
            }
            android {
                buildTypes {
                    release {
                        minifyEnabled false
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        assertEquals(false, result.configs.single().releaseMinifyEnabled)
    }

    @Test
    fun extractsGroovyReleaseShrinkResourcesTrue() = withGradleFile(
        fileName = "build.gradle",
        content = """
            plugins {
                id 'com.android.application'
            }
            android {
                buildTypes {
                    release {
                        shrinkResources true
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        assertEquals(true, result.configs.single().releaseShrinkResources)
    }

    @Test
    fun detectsKotlinDslAndroidApplicationPlugin() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            plugins {
                id("com.android.application")
            }
        """.trimIndent()
    ) { result ->
        val config = result.configs.single()
        assertEquals(true, config.hasAndroidPlugin)
        assertEquals(AndroidPluginType.APPLICATION, config.androidPluginType)
    }

    @Test
    fun detectsKotlinDslAndroidLibraryPlugin() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            plugins {
                id("com.android.library")
            }
        """.trimIndent()
    ) { result ->
        val config = result.configs.single()
        assertEquals(true, config.hasAndroidPlugin)
        assertEquals(AndroidPluginType.LIBRARY, config.androidPluginType)
    }

    @Test
    fun detectsGroovyAndroidApplicationPlugin() = withGradleFile(
        fileName = "build.gradle",
        content = """
            plugins {
                id 'com.android.application'
            }
        """.trimIndent()
    ) { result ->
        val config = result.configs.single()
        assertEquals(true, config.hasAndroidPlugin)
        assertEquals(AndroidPluginType.APPLICATION, config.androidPluginType)
    }

    @Test
    fun detectsGroovyAndroidLibraryPlugin() = withGradleFile(
        fileName = "build.gradle",
        content = """
            plugins {
                id 'com.android.library'
            }
        """.trimIndent()
    ) { result ->
        val config = result.configs.single()
        assertEquals(true, config.hasAndroidPlugin)
        assertEquals(AndroidPluginType.LIBRARY, config.androidPluginType)
    }

    @Test
    fun detectsApplyPluginAndroidApplication() = withGradleFile(
        fileName = "build.gradle",
        content = "apply plugin: 'com.android.application'"
    ) { result ->
        val config = result.configs.single()
        assertEquals(true, config.hasAndroidPlugin)
        assertEquals(AndroidPluginType.APPLICATION, config.androidPluginType)
    }

    @Test
    fun detectsApplyPluginAndroidLibrary() = withGradleFile(
        fileName = "build.gradle",
        content = "apply plugin: 'com.android.library'"
    ) { result ->
        val config = result.configs.single()
        assertEquals(true, config.hasAndroidPlugin)
        assertEquals(AndroidPluginType.LIBRARY, config.androidPluginType)
    }

    @Test
    fun detectsKotlinDslAliasAndroidApplicationPlugin() = withGradleFile(
        fileName = "build.gradle.kts",
        content = "alias(libs.plugins.android.application)"
    ) { result ->
        val config = result.configs.single()
        assertEquals(true, config.hasAndroidPlugin)
        assertEquals(AndroidPluginType.APPLICATION, config.androidPluginType)
    }

    @Test
    fun detectsKotlinDslAliasAndroidLibraryPlugin() = withGradleFile(
        fileName = "build.gradle.kts",
        content = "alias libs.plugins.android.library"
    ) { result ->
        val config = result.configs.single()
        assertEquals(true, config.hasAndroidPlugin)
        assertEquals(AndroidPluginType.LIBRARY, config.androidPluginType)
    }

    @Test
    fun detectsConventionAndroidLibraryPluginAsUnknownAndroid() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            plugins {
                id("company.android.library")
            }
        """.trimIndent()
    ) { result ->
        val config = result.configs.single()
        assertEquals(true, config.hasAndroidPlugin)
        assertEquals(AndroidPluginType.UNKNOWN_ANDROID, config.androidPluginType)
    }

    @Test
    fun ignoresNonAndroidGradleFile() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            plugins {
                kotlin("jvm")
            }
        """.trimIndent()
    ) { result ->
        assertEquals(1, result.scannedGradleFileCount)
        assertTrue(result.failedGradleFiles.isEmpty())
        assertTrue(result.configs.isEmpty())
    }

    @Test
    fun keepsVariableBasedSdkValuesAsNull() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            plugins {
                alias libs.plugins.android.library
            }
            android {
                namespace = "com.example.app"
                compileSdk = libs.versions.compileSdk.get().toInt()
                defaultConfig {
                    minSdk = minSdkVersion
                }
            }
        """.trimIndent()
    ) { result ->
        val config = result.configs.single()
        assertEquals(true, config.hasAndroidPlugin)
        assertEquals(AndroidPluginType.LIBRARY, config.androidPluginType)
        assertEquals("com.example.app", config.namespace)
        assertNull(config.compileSdk)
        assertNull(config.minSdk)
    }

    @Test
    fun returnsNullForVariableBasedReleaseMinifyEnabled() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            plugins {
                id("com.android.application")
            }
            android {
                buildTypes {
                    release {
                        isMinifyEnabled = releaseMinifyFlag
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        val config = result.configs.single()
        assertNull(config.releaseMinifyEnabled)
    }

    @Test
    fun doesNotUseDebugMinifyEnabledAsReleaseMinifyEnabled() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            plugins {
                id("com.android.application")
            }
            android {
                buildTypes {
                    debug {
                        isMinifyEnabled = false
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        val config = result.configs.single()
        assertEquals(false, config.minifyEnabled)
        assertNull(config.releaseMinifyEnabled)
    }

    @Test
    fun doesNotUseStagingMinifyEnabledAsReleaseMinifyEnabled() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            plugins {
                id("com.android.application")
            }
            android {
                buildTypes {
                    staging {
                        isMinifyEnabled = false
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        val config = result.configs.single()
        assertEquals(false, config.minifyEnabled)
        assertNull(config.releaseMinifyEnabled)
    }

    @Test
    fun doesNotUseReleaseCandidateMinifyEnabledAsReleaseMinifyEnabled() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            plugins {
                id("com.android.application")
            }
            android {
                buildTypes {
                    releaseCandidate {
                        isMinifyEnabled = false
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        val config = result.configs.single()
        assertEquals(false, config.minifyEnabled)
        assertNull(config.releaseMinifyEnabled)
    }

    private fun withGradleFile(
        fileName: String,
        content: String,
        assertion: (GradleAndroidConfigScanResult) -> Unit
    ) {
        val directory = Files.createTempDirectory("releasely-gradle-config-test")
        try {
            val gradleFile = directory.resolve(fileName)
            gradleFile.writeText(content)

            assertion(scanner.scan(listOf(gradleFile)))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
