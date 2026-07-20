package io.github.sandroisu.releasely

import io.github.sandroisu.releasely.rules.MinifyDisabledReleaseRule
import io.github.sandroisu.releasely.rules.ReleaseRuleContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
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
    fun extractsKotlinDslReleaseDebuggableTrue() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            plugins {
                id("com.android.application")
            }
            android {
                buildTypes {
                    release {
                        isDebuggable = true
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        assertEquals(true, result.configs.single().releaseDebuggable)
    }

    @Test
    fun extractsKotlinDslGetByNameReleaseDebuggableTrue() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            plugins {
                id("com.android.application")
            }
            android {
                buildTypes {
                    getByName("release") {
                        isDebuggable = true
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        assertEquals(true, result.configs.single().releaseDebuggable)
    }

    @Test
    fun extractsKotlinDslReleaseDebuggableFalse() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            plugins {
                id("com.android.application")
            }
            android {
                buildTypes {
                    release {
                        isDebuggable = false
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        assertEquals(false, result.configs.single().releaseDebuggable)
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
    fun extractsGroovyReleaseDebuggableTrue() = withGradleFile(
        fileName = "build.gradle",
        content = """
            plugins {
                id 'com.android.application'
            }
            android {
                buildTypes {
                    release {
                        debuggable true
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        assertEquals(true, result.configs.single().releaseDebuggable)
    }

    @Test
    fun doesNotUseGroovyDebugMinifyEnabledAsReleaseMinifyEnabled() = withGradleFile(
        fileName = "build.gradle",
        content = """
            plugins {
                id 'com.android.application'
            }
            android {
                buildTypes {
                    debug {
                        minifyEnabled false
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
    fun doesNotUseDebugDebuggableAsReleaseDebuggable() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            plugins {
                id("com.android.application")
            }
            android {
                buildTypes {
                    debug {
                        isDebuggable = true
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        assertNull(result.configs.single().releaseDebuggable)
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
    fun returnsNullForVariableBasedReleaseDebuggable() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            plugins {
                id("com.android.application")
            }
            android {
                buildTypes {
                    release {
                        isDebuggable = releaseDebuggableFlag
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        assertNull(result.configs.single().releaseDebuggable)
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
    fun doesNotUseStagingDebuggableAsReleaseDebuggable() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            plugins {
                id("com.android.application")
            }
            android {
                buildTypes {
                    staging {
                        isDebuggable = true
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        assertNull(result.configs.single().releaseDebuggable)
    }

    @Test
    fun doesNotUseReleaseCandidateDebuggableAsReleaseDebuggable() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            plugins {
                id("com.android.application")
            }
            android {
                buildTypes {
                    releaseCandidate {
                        isDebuggable = true
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        assertNull(result.configs.single().releaseDebuggable)
    }

    @Test
    fun doesNotUseDebuggableOutsideReleaseBuildType() = withGradleFile(
        fileName = "build.gradle.kts",
        content = """
            val debuggable = true

            plugins {
                id("com.android.application")
            }
            android {
                buildTypes {
                    debug {
                    }
                }
            }
        """.trimIndent()
    ) { result ->
        assertNull(result.configs.single().releaseDebuggable)
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

    @Test
    fun resolvesApplicationPluginViaCatalogAliasWithoutAndroidInAccessorName() = withGradleProject(
        catalog = """
            [plugins]
            app = { id = "com.android.application", version.ref = "agp" }
        """.trimIndent(),
        moduleBuildFile = "alias(libs.plugins.app)"
    ) { result ->
        val config = result.configs.single()
        assertEquals(true, config.hasAndroidPlugin)
        assertEquals(AndroidPluginType.APPLICATION, config.androidPluginType)
    }

    @Test
    fun resolvesLibraryPluginViaCatalogAliasWithoutAndroidInAccessorName() = withGradleProject(
        catalog = """
            [plugins]
            sharedModule = { id = "com.android.library", version.ref = "agp" }
        """.trimIndent(),
        moduleBuildFile = "alias(libs.plugins.sharedModule)"
    ) { result ->
        val config = result.configs.single()
        assertEquals(true, config.hasAndroidPlugin)
        assertEquals(AndroidPluginType.LIBRARY, config.androidPluginType)
    }

    @Test
    fun resolvesThreeTimesADayAliasForm() = withGradleProject(
        catalog = """
            [versions]
            agp = "8.7.0"

            [plugins]
            androidApplication = { id = "com.android.application", version.ref = "agp" }
            composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "agp" }
        """.trimIndent(),
        moduleBuildFile = """
            plugins {
                alias(libs.plugins.androidApplication)
                alias(libs.plugins.composeMultiplatform)
            }
            android {
                namespace = "io.github.sandroisu.threetimesaday"
            }
        """.trimIndent()
    ) { result ->
        val config = result.configs.single()
        assertEquals(true, config.hasAndroidPlugin)
        assertEquals(AndroidPluginType.APPLICATION, config.androidPluginType)
    }

    @Test
    fun resolvesDashedCatalogAliasThroughGeneratedDottedAccessor() = withGradleProject(
        catalog = """
            [plugins]
            android-application = { id = "com.android.application", version.ref = "agp" }
        """.trimIndent(),
        moduleBuildFile = "alias(libs.plugins.android.application)"
    ) { result ->
        val config = result.configs.single()
        assertEquals(true, config.hasAndroidPlugin)
        assertEquals(AndroidPluginType.APPLICATION, config.androidPluginType)
    }

    @Test
    fun rootAliasWithApplyFalseDoesNotBecomeAndroidModule() = withGradleProject(
        catalog = """
            [plugins]
            androidApplication = { id = "com.android.application", version.ref = "agp" }
            androidLibrary = { id = "com.android.library", version.ref = "agp" }
        """.trimIndent(),
        moduleBuildFile = """
            plugins {
                alias(libs.plugins.androidApplication) apply false
                alias(libs.plugins.androidLibrary) apply false
            }
        """.trimIndent()
    ) { result ->
        assertEquals(1, result.scannedGradleFileCount)
        assertTrue(result.failedGradleFiles.isEmpty())
        assertTrue(result.configs.isEmpty())
    }

    @Test
    fun unknownCatalogAliasIdStaysUnknownAndroid() = withGradleProject(
        catalog = """
            [plugins]
            androidApplication = { id = "com.acme.android.custom", version.ref = "agp" }
        """.trimIndent(),
        moduleBuildFile = """
            plugins {
                alias(libs.plugins.androidApplication)
            }
            android {
                namespace = "com.acme.app"
            }
        """.trimIndent()
    ) { result ->
        val config = result.configs.single()
        assertEquals(true, config.hasAndroidPlugin)
        assertEquals(AndroidPluginType.UNKNOWN_ANDROID, config.androidPluginType)
    }

    @Test
    fun missingCatalogFallsBackToAccessorHeuristic() = withGradleFile(
        fileName = "build.gradle.kts",
        content = "alias(libs.plugins.android.application)"
    ) { result ->
        assertTrue(result.failedGradleFiles.isEmpty())
        val config = result.configs.single()
        assertEquals(true, config.hasAndroidPlugin)
        assertEquals(AndroidPluginType.APPLICATION, config.androidPluginType)
    }

    @Test
    fun keepsDirectPluginIdWhenCatalogIsPresent() = withGradleProject(
        catalog = """
            [plugins]
            androidApplication = { id = "com.android.application", version.ref = "agp" }
        """.trimIndent(),
        moduleBuildFile = """
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
    fun applicationOnlyRuleRunsAfterApplicationResolvedViaAlias() = withGradleProject(
        catalog = """
            [plugins]
            androidApplication = { id = "com.android.application", version.ref = "agp" }
        """.trimIndent(),
        moduleBuildFile = """
            plugins {
                alias(libs.plugins.androidApplication)
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
        val config = result.configs.single()
        assertEquals(AndroidPluginType.APPLICATION, config.androidPluginType)
        assertEquals(false, config.releaseMinifyEnabled)

        val findings = MinifyDisabledReleaseRule().evaluate(
            ReleaseRuleContext(
                projectPath = config.gradleFile.parent,
                permissions = emptyList(),
                manifestComponents = emptyList(),
                gradleAndroidConfigs = result.configs
            )
        )
        assertEquals(1, findings.size)
        assertEquals("gradle.release.minify_disabled", findings.single().ruleId)
    }

    private fun withGradleProject(
        catalog: String,
        moduleBuildFile: String,
        moduleDirectoryName: String = "androidApp",
        assertion: (GradleAndroidConfigScanResult) -> Unit
    ) {
        val projectRoot = Files.createTempDirectory("releasely-gradle-project-test")
        try {
            val catalogFile = projectRoot.resolve("gradle").resolve("libs.versions.toml")
            catalogFile.parent.createDirectories()
            catalogFile.writeText(catalog)

            val moduleDirectory = projectRoot.resolve(moduleDirectoryName)
            moduleDirectory.createDirectories()
            val buildFile = moduleDirectory.resolve("build.gradle.kts")
            buildFile.writeText(moduleBuildFile)

            assertion(scanner.scan(listOf(buildFile)))
        } finally {
            projectRoot.toFile().deleteRecursively()
        }
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
