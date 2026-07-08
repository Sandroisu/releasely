package io.github.sandroisu.releasely

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MinifyDisabledReleaseRuleTest {

    private val rule = MinifyDisabledReleaseRule()

    @Test
    fun returnsMediumFindingForApplicationModuleWithMinifyDisabled() {
        val finding = rule.evaluate(contextWith(config(AndroidPluginType.APPLICATION, hasAndroidPlugin = true, minifyEnabled = false))).single()

        assertEquals("gradle.release.minify_disabled", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.MEDIUM, finding.severity)
        assertEquals("Release minification appears disabled", finding.title)
        assertTrue(finding.evidence.contains("minifyEnabled=false"))
    }

    @Test
    fun ignoresApplicationModuleWithMinifyEnabled() {
        val findings = rule.evaluate(contextWith(config(AndroidPluginType.APPLICATION, hasAndroidPlugin = true, minifyEnabled = true)))

        assertTrue(findings.isEmpty())
    }

    @Test
    fun ignoresApplicationModuleWithMissingMinifyValue() {
        val findings = rule.evaluate(contextWith(config(AndroidPluginType.APPLICATION, hasAndroidPlugin = true, minifyEnabled = null)))

        assertTrue(findings.isEmpty())
    }

    @Test
    fun ignoresLibraryModuleWithMinifyDisabled() {
        val findings = rule.evaluate(contextWith(config(AndroidPluginType.LIBRARY, hasAndroidPlugin = true, minifyEnabled = false)))

        assertTrue(findings.isEmpty())
    }

    @Test
    fun ignoresDynamicFeatureModuleWithMinifyDisabled() {
        val findings = rule.evaluate(contextWith(config(AndroidPluginType.DYNAMIC_FEATURE, hasAndroidPlugin = true, minifyEnabled = false)))

        assertTrue(findings.isEmpty())
    }

    @Test
    fun ignoresTestModuleWithMinifyDisabled() {
        val findings = rule.evaluate(contextWith(config(AndroidPluginType.TEST, hasAndroidPlugin = true, minifyEnabled = false)))

        assertTrue(findings.isEmpty())
    }

    @Test
    fun ignoresUnknownAndroidModuleWithMinifyDisabled() {
        val findings = rule.evaluate(contextWith(config(AndroidPluginType.UNKNOWN_ANDROID, hasAndroidPlugin = true, minifyEnabled = false)))
        
        assertTrue(findings.isEmpty())
    }

    @Test
    fun ignoresNonAndroidConfigWithMinifyDisabled() {
        val findings = rule.evaluate(contextWith(config(null, hasAndroidPlugin = false, minifyEnabled = false)))

        assertTrue(findings.isEmpty())
    }

    @Test
    fun canBeUsedThroughReleaseRuleInterface() {
        val releaseRule: ReleaseRule = rule

        val finding = releaseRule.evaluate(contextWith(config(AndroidPluginType.APPLICATION, hasAndroidPlugin = true, minifyEnabled = false))).single()

        assertEquals("gradle.release.minify_disabled", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.MEDIUM, finding.severity)
    }

    private fun contextWith(vararg configs: GradleAndroidConfig): ReleaseRuleContext =
        ReleaseRuleContext(
            projectPath = Path.of("."),
            permissions = emptyList(),
            manifestComponents = emptyList(),
            gradleAndroidConfigs = configs.toList()
        )

    private fun config(
        androidPluginType: AndroidPluginType?,
        hasAndroidPlugin: Boolean,
        minifyEnabled: Boolean?
    ): GradleAndroidConfig =
        GradleAndroidConfig(
            gradleFile = Path.of("app/build.gradle.kts"),
            androidPluginType = androidPluginType,
            hasAndroidPlugin = hasAndroidPlugin,
            applicationId = "com.example.app",
            namespace = "com.example.app",
            compileSdk = 35,
            minSdk = 26,
            targetSdk = 35,
            versionCode = 123,
            versionName = "1.2.3",
            minifyEnabled = minifyEnabled,
            shrinkResources = null
        )
}
