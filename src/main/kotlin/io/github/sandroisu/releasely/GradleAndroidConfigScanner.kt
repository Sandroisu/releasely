package io.github.sandroisu.releasely

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

data class GradleAndroidConfigScanResult(
    val scannedGradleFileCount: Int,
    val failedGradleFiles: List<Path>,
    val configs: List<GradleAndroidConfig>
)

class GradleAndroidConfigScanner {

    private companion object {
        private val androidBlockPattern = Regex("""\bandroid\s*\{""")
        private val kotlinDslPluginPattern = Regex("""\bid\(\s*"([^"]+)"\s*\)""")
        private val groovyDslPluginPattern = Regex("""\bid\s+['"]([^'"]+)['"]""")
        private val applyPluginPattern = Regex("""\bapply\s+plugin:\s*['"]([^'"]+)['"]""")
        private val aliasFunctionPattern = Regex("""\balias\(\s*libs\.plugins\.([A-Za-z0-9_.-]+)\s*\)""")
        private val aliasPropertyPattern = Regex("""\balias\s+libs\.plugins\.([A-Za-z0-9_.-]+)\b""")
        private val stringAssignmentPatterns = mapOf(
            "applicationId" to Regex("""\bapplicationId\s*(?:=|\s)\s*"([^"]+)""""),
            "namespace" to Regex("""\bnamespace\s*(?:=|\s)\s*"([^"]+)""""),
            "versionName" to Regex("""\bversionName\s*(?:=|\s)\s*"([^"]+)"""")
        )

        private val intAssignmentPatterns = mapOf(
            "compileSdk" to Regex("""\b(?:compileSdk|compileSdkVersion)\s*(?:=|\s)\s*(\d+)\b"""),
            "minSdk" to Regex("""\b(?:minSdk|minSdkVersion)\s*(?:=|\s)\s*(\d+)\b"""),
            "targetSdk" to Regex("""\b(?:targetSdk|targetSdkVersion)\s*(?:=|\s)\s*(\d+)\b"""),
            "versionCode" to Regex("""\bversionCode\s*(?:=|\s)\s*(\d+)\b""")
        )

        private val booleanAssignmentPatterns = mapOf(
            "minifyEnabled" to Regex("""\b(?:isMinifyEnabled|minifyEnabled)\s*(?:=|\s)\s*(true|false)\b"""),
            "shrinkResources" to Regex("""\b(?:isShrinkResources|shrinkResources)\s*(?:=|\s)\s*(true|false)\b""")
        )

        private val defaultConfigHeader = Regex("""\bdefaultConfig\s*\{""")
        private val buildTypesHeader = Regex("""\bbuildTypes\s*\{""")
        private val releaseHeader = Regex(
            """(?:\brelease\b|getByName\(\s*"release"\s*\)|named\(\s*"release"\s*\)|create\(\s*"release"\s*\)|maybeCreate\(\s*"release"\s*\))\s*\{"""
        )
    }

    fun scan(gradleFiles: List<Path>): GradleAndroidConfigScanResult {
        val filesToScan = gradleFiles
            .filter(Files::isRegularFile)
            .filter(::isAndroidModuleBuildFile)

        val configs = mutableListOf<GradleAndroidConfig>()
        val failedGradleFiles = mutableListOf<Path>()

        filesToScan.forEach { gradleFile ->
            try {
                val fileContent = Files.readString(gradleFile)
                parseConfig(gradleFile, fileContent)?.let(configs::add)
            } catch (_: Exception) {
                failedGradleFiles.add(gradleFile)
            }
        }

        return GradleAndroidConfigScanResult(
            scannedGradleFileCount = filesToScan.size,
            failedGradleFiles = failedGradleFiles,
            configs = configs
        )
    }

    private fun isAndroidModuleBuildFile(gradleFile: Path): Boolean =
        gradleFile.name == "build.gradle" || gradleFile.name == "build.gradle.kts"

    private fun parseConfig(gradleFile: Path, fileContent: String): GradleAndroidConfig? {
        val androidPluginType = detectAndroidPluginType(fileContent)
        val hasAndroidPlugin = androidPluginType != null

        if (!hasAndroidPlugin && !looksLikeAndroidGradleFile(fileContent)) {
            return null
        }

        val defaultConfigBlock = extractBlock(fileContent, defaultConfigHeader)
        val buildTypesBlock = extractBlock(fileContent, buildTypesHeader)
        val releaseBuildTypeBlock = buildTypesBlock?.let { buildTypesText ->
            extractBlock(buildTypesText, releaseHeader)
        }

        val config = GradleAndroidConfig(
            gradleFile = gradleFile,
            androidPluginType = androidPluginType,
            hasAndroidPlugin = hasAndroidPlugin,
            applicationId = extractString("applicationId", defaultConfigBlock, fileContent),
            namespace = extractString("namespace", fileContent),
            compileSdk = extractInt("compileSdk", fileContent),
            minSdk = extractInt("minSdk", defaultConfigBlock, fileContent),
            targetSdk = extractInt("targetSdk", defaultConfigBlock, fileContent),
            versionCode = extractInt("versionCode", defaultConfigBlock, fileContent),
            versionName = extractString("versionName", defaultConfigBlock, fileContent),
            minifyEnabled = extractBoolean("minifyEnabled", releaseBuildTypeBlock ?: fileContent),
            shrinkResources = extractBoolean("shrinkResources", releaseBuildTypeBlock ?: fileContent),
            releaseMinifyEnabled = extractBooleanOrNull("minifyEnabled", releaseBuildTypeBlock),
            releaseShrinkResources = extractBooleanOrNull("shrinkResources", releaseBuildTypeBlock)
        )

        return config.takeIf { parsedConfig ->
            parsedConfig.hasAndroidPlugin ||
                parsedConfig.applicationId != null ||
                parsedConfig.namespace != null ||
                parsedConfig.compileSdk != null ||
                parsedConfig.minSdk != null ||
                parsedConfig.targetSdk != null ||
                parsedConfig.versionCode != null ||
                parsedConfig.versionName != null ||
                parsedConfig.minifyEnabled != null ||
                parsedConfig.shrinkResources != null ||
                parsedConfig.releaseMinifyEnabled != null ||
                parsedConfig.releaseShrinkResources != null
        }
    }

    private fun detectAndroidPluginType(fileContent: String): AndroidPluginType? {
        val detectedTypes = fileContent
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .filterNot { line -> line.contains("apply false") }
            .mapNotNull(::detectAndroidPluginTypeFromLine)
            .toList()

        return detectedTypes.firstOrNull { pluginType -> pluginType != AndroidPluginType.UNKNOWN_ANDROID }
            ?: detectedTypes.firstOrNull()
    }

    private fun detectAndroidPluginTypeFromLine(line: String): AndroidPluginType? {
        kotlinDslPluginPattern.find(line)?.groupValues?.getOrNull(1)?.let(::pluginTypeFromPluginId)?.let { return it }
        groovyDslPluginPattern.find(line)?.groupValues?.getOrNull(1)?.let(::pluginTypeFromPluginId)?.let { return it }
        applyPluginPattern.find(line)?.groupValues?.getOrNull(1)?.let(::pluginTypeFromPluginId)?.let { return it }
        aliasFunctionPattern.find(line)?.groupValues?.getOrNull(1)?.let(::pluginTypeFromAlias)?.let { return it }
        aliasPropertyPattern.find(line)?.groupValues?.getOrNull(1)?.let(::pluginTypeFromAlias)?.let { return it }
        return null
    }

    private fun pluginTypeFromPluginId(pluginId: String): AndroidPluginType? =
        when (pluginId) {
            "com.android.application" -> AndroidPluginType.APPLICATION
            "com.android.library" -> AndroidPluginType.LIBRARY
            "com.android.dynamic-feature" -> AndroidPluginType.DYNAMIC_FEATURE
            "com.android.test" -> AndroidPluginType.TEST
            else -> if (".android." in pluginId) {
                AndroidPluginType.UNKNOWN_ANDROID
            } else {
                null
            }
        }

    private fun pluginTypeFromAlias(aliasName: String): AndroidPluginType? =
        when {
            aliasName.contains("android.application") -> AndroidPluginType.APPLICATION
            aliasName.contains("android.library") -> AndroidPluginType.LIBRARY
            aliasName.contains("android") -> AndroidPluginType.UNKNOWN_ANDROID
            else -> null
        }

    private fun looksLikeAndroidGradleFile(fileContent: String): Boolean =
        fileContent.contains("com.android.") ||
            fileContent.contains("alias(libs.plugins.android") ||
            fileContent.contains("alias libs.plugins.android") ||
            fileContent.contains("id(\"com.android") ||
            fileContent.contains("id 'com.android") ||
            fileContent.contains("apply plugin: 'com.android") ||
            fileContent.contains("apply plugin: \"com.android") ||
            androidBlockPattern.containsMatchIn(fileContent)

    private fun extractString(propertyName: String, primaryText: String?, fallbackText: String): String? =
        extractString(propertyName, primaryText) ?: extractString(propertyName, fallbackText)

    private fun extractString(propertyName: String, text: String?): String? =
        text
            ?.let(stringAssignmentPatterns.getValue(propertyName)::find)
            ?.groupValues
            ?.getOrNull(1)

    private fun extractInt(propertyName: String, primaryText: String?, fallbackText: String): Int? =
        extractInt(propertyName, primaryText) ?: extractInt(propertyName, fallbackText)

    private fun extractInt(propertyName: String, text: String?): Int? =
        text
            ?.let(intAssignmentPatterns.getValue(propertyName)::find)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

    private fun extractBoolean(propertyName: String, text: String): Boolean? =
        booleanAssignmentPatterns.getValue(propertyName)
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.toBooleanStrictOrNull()

    private fun extractBooleanOrNull(propertyName: String, text: String?): Boolean? =
        text?.let(::normalizeBooleanSource)?.let(booleanAssignmentPatterns.getValue(propertyName)::find)
            ?.groupValues
            ?.getOrNull(1)
            ?.toBooleanStrictOrNull()

    private fun normalizeBooleanSource(text: String): String =
        text.lineSequence()
            .map(String::trim)
            .joinToString("\n")

    private fun extractBlock(text: String, headerPattern: Regex): String? {
        val match = headerPattern.find(text) ?: return null
        val blockStartIndex = text.indexOf('{', match.range.first)
        if (blockStartIndex < 0) {
            return null
        }

        var depth = 0
        for (index in blockStartIndex until text.length) {
            when (text[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return text.substring(blockStartIndex + 1, index)
                    }
                }
            }
        }

        return null
    }
}
