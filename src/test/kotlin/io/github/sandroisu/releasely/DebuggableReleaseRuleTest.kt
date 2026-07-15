package io.github.sandroisu.releasely

import io.github.sandroisu.releasely.rules.DebuggableReleaseRule
import io.github.sandroisu.releasely.rules.ReleaseRuleContext
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DebuggableReleaseRuleTest {

    private val rule = DebuggableReleaseRule()

    @Test
    fun returnsHighFindingForApplicationModuleWithReleaseDebuggableEnabled() {
        val findings = rule.evaluate(
            contextWith(
                config(
                    androidPluginType = AndroidPluginType.APPLICATION,
                    hasAndroidPlugin = true,
                    releaseDebuggable = true
                )
            )
        )

        val finding = findings.single()
        assertEquals("gradle.release.debuggable_enabled", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.HIGH, finding.severity)
        assertEquals("Release build is debuggable", finding.title)
        assertEquals(
            listOf(
                "Gradle file: app/build.gradle.kts",
                "Android plugin type: APPLICATION",
                "releaseDebuggable=true"
            ),
            finding.evidence.map { evidence -> evidence.replace('\\', '/') }
        )
        assertEquals("app/build.gradle.kts", finding.locationPath?.replace('\\', '/'))
    }

    @Test
    fun ignoresApplicationModuleWithReleaseDebuggableDisabled() {
        val findings = rule.evaluate(
            contextWith(
                config(
                    androidPluginType = AndroidPluginType.APPLICATION,
                    hasAndroidPlugin = true,
                    releaseDebuggable = false
                )
            )
        )

        assertTrue(findings.isEmpty())
    }

    @Test
    fun ignoresApplicationModuleWithMissingReleaseDebuggableValue() {
        val findings = rule.evaluate(
            contextWith(
                config(
                    androidPluginType = AndroidPluginType.APPLICATION,
                    hasAndroidPlugin = true,
                    releaseDebuggable = null
                )
            )
        )

        assertTrue(findings.isEmpty())
    }

    @Test
    fun ignoresNonApplicationPluginTypesAndNonAndroidConfigs() {
        val configs = AndroidPluginType.entries
            .filterNot { pluginType -> pluginType == AndroidPluginType.APPLICATION }
            .map { pluginType ->
                config(
                    androidPluginType = pluginType,
                    hasAndroidPlugin = true,
                    releaseDebuggable = true
                )
            } + config(
                androidPluginType = null,
                hasAndroidPlugin = false,
                releaseDebuggable = true
            )

        assertTrue(rule.evaluate(contextWith(*configs.toTypedArray())).isEmpty())
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
        releaseDebuggable: Boolean?
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
            minifyEnabled = null,
            shrinkResources = null,
            releaseMinifyEnabled = null,
            releaseShrinkResources = null,
            releaseDebuggable = releaseDebuggable
        )
}
