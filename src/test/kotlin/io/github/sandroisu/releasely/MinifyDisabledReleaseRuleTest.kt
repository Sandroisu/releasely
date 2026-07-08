package io.github.sandroisu.releasely

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MinifyDisabledReleaseRuleTest {

    private val rule = MinifyDisabledReleaseRule()

    @Test
    fun returnsMediumFindingForApplicationModuleWithReleaseMinifyDisabled() {
        val finding = rule.evaluate(
            contextWith(
                config(
                    androidPluginType = AndroidPluginType.APPLICATION,
                    hasAndroidPlugin = true,
                    minifyEnabled = true,
                    releaseMinifyEnabled = false
                )
            )
        ).single()

        assertEquals("gradle.release.minify_disabled", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.MEDIUM, finding.severity)
        assertEquals("Release minification appears disabled", finding.title)
        assertTrue(finding.evidence.contains("releaseMinifyEnabled=false"))
    }

    @Test
    fun ignoresApplicationModuleWithOnlyGenericMinifyDisabled() {
        val findings = rule.evaluate(
            contextWith(
                config(
                    androidPluginType = AndroidPluginType.APPLICATION,
                    hasAndroidPlugin = true,
                    minifyEnabled = false,
                    releaseMinifyEnabled = null
                )
            )
        )

        assertTrue(findings.isEmpty())
    }

    @Test
    fun ignoresApplicationModuleWithReleaseMinifyEnabled() {
        val findings = rule.evaluate(
            contextWith(
                config(
                    androidPluginType = AndroidPluginType.APPLICATION,
                    hasAndroidPlugin = true,
                    minifyEnabled = false,
                    releaseMinifyEnabled = true
                )
            )
        )

        assertTrue(findings.isEmpty())
    }

    @Test
    fun ignoresApplicationModuleWithMissingReleaseMinifyValue() {
        val findings = rule.evaluate(
            contextWith(
                config(
                    androidPluginType = AndroidPluginType.APPLICATION,
                    hasAndroidPlugin = true,
                    minifyEnabled = null,
                    releaseMinifyEnabled = null
                )
            )
        )

        assertTrue(findings.isEmpty())
    }

    @Test
    fun ignoresLibraryModuleWithReleaseMinifyDisabled() {
        val findings = rule.evaluate(
            contextWith(
                config(
                    androidPluginType = AndroidPluginType.LIBRARY,
                    hasAndroidPlugin = true,
                    minifyEnabled = false,
                    releaseMinifyEnabled = false
                )
            )
        )

        assertTrue(findings.isEmpty())
    }

    @Test
    fun ignoresDynamicFeatureModuleWithReleaseMinifyDisabled() {
        val findings = rule.evaluate(
            contextWith(
                config(
                    androidPluginType = AndroidPluginType.DYNAMIC_FEATURE,
                    hasAndroidPlugin = true,
                    minifyEnabled = false,
                    releaseMinifyEnabled = false
                )
            )
        )

        assertTrue(findings.isEmpty())
    }

    @Test
    fun ignoresTestModuleWithReleaseMinifyDisabled() {
        val findings = rule.evaluate(
            contextWith(
                config(
                    androidPluginType = AndroidPluginType.TEST,
                    hasAndroidPlugin = true,
                    minifyEnabled = false,
                    releaseMinifyEnabled = false
                )
            )
        )

        assertTrue(findings.isEmpty())
    }

    @Test
    fun ignoresUnknownAndroidModuleWithReleaseMinifyDisabled() {
        val findings = rule.evaluate(
            contextWith(
                config(
                    androidPluginType = AndroidPluginType.UNKNOWN_ANDROID,
                    hasAndroidPlugin = true,
                    minifyEnabled = false,
                    releaseMinifyEnabled = false
                )
            )
        )

        assertTrue(findings.isEmpty())
    }

    @Test
    fun canBeUsedThroughReleaseRuleInterface() {
        val releaseRule: ReleaseRule = rule

        val finding = releaseRule.evaluate(
            contextWith(
                config(
                    androidPluginType = AndroidPluginType.APPLICATION,
                    hasAndroidPlugin = true,
                    minifyEnabled = true,
                    releaseMinifyEnabled = false
                )
            )
        ).single()

        assertEquals("gradle.release.minify_disabled", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.MEDIUM, finding.severity)
    }

    @Test
    fun ignoresNonAndroidConfigWithMinifyDisabled() {
        val findings = rule.evaluate(
            contextWith(
                config(
                    androidPluginType = null,
                    hasAndroidPlugin = false,
                    minifyEnabled = false,
                    releaseMinifyEnabled = false
                )
            )
        )

        assertTrue(findings.isEmpty())
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
        minifyEnabled: Boolean?,
        releaseMinifyEnabled: Boolean?
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
            shrinkResources = null,
            releaseMinifyEnabled = releaseMinifyEnabled,
            releaseShrinkResources = null
        )
}
