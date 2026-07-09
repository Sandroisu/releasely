package io.github.sandroisu.releasely

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReleaseRuleEvaluatorTest {

    @Test
    fun baselineSuccessRunsPermissionComponentAndGradleFindings() {
        val evaluator = ReleaseRuleEvaluator(
            permissionRules = listOf(findingRule("permission", "perm-1")),
            componentRules = listOf(findingRule("component", "component-1")),
            gradleRules = listOf(findingRule("gradle", "gradle-1"))
        )

        val findings = evaluator.evaluate(
            projectPath = Path.of("."),
            currentPermissions = listOf("perm-1"),
            baselinePermissions = emptyList(),
            manifestComponents = listOf(component()),
            gradleAndroidConfigs = listOf(config())
        )

        assertEquals(listOf("permission", "component", "gradle"), findings.map(ReleaseFinding::ruleId))
    }

    @Test
    fun baselineFailureDoesNotRunPermissionFindings() {
        val permissionRule = recordingRule("permission")
        val evaluator = ReleaseRuleEvaluator(
            permissionRules = listOf(permissionRule),
            componentRules = emptyList(),
            gradleRules = emptyList()
        )

        val findings = evaluator.evaluate(
            projectPath = Path.of("."),
            currentPermissions = listOf("perm-1"),
            baselinePermissions = null,
            manifestComponents = emptyList(),
            gradleAndroidConfigs = emptyList()
        )

        assertTrue(findings.isEmpty())
        assertFalse(permissionRule.wasCalled)
    }

    @Test
    fun baselineFailureStillRunsComponentFindings() {
        val componentRule = recordingRule("component")
        val evaluator = ReleaseRuleEvaluator(
            permissionRules = emptyList(),
            componentRules = listOf(componentRule),
            gradleRules = emptyList()
        )

        val findings = evaluator.evaluate(
            projectPath = Path.of("."),
            currentPermissions = emptyList(),
            baselinePermissions = null,
            manifestComponents = listOf(component()),
            gradleAndroidConfigs = emptyList()
        )

        assertEquals(listOf("component"), findings.map(ReleaseFinding::ruleId))
        assertTrue(componentRule.wasCalled)
    }

    @Test
    fun baselineFailureStillRunsGradleFindings() {
        val gradleRule = recordingRule("gradle")
        val evaluator = ReleaseRuleEvaluator(
            permissionRules = emptyList(),
            componentRules = emptyList(),
            gradleRules = listOf(gradleRule)
        )

        val findings = evaluator.evaluate(
            projectPath = Path.of("."),
            currentPermissions = emptyList(),
            baselinePermissions = null,
            manifestComponents = emptyList(),
            gradleAndroidConfigs = listOf(config())
        )

        assertEquals(listOf("gradle"), findings.map(ReleaseFinding::ruleId))
        assertTrue(gradleRule.wasCalled)
    }

    @Test
    fun baselineFailureStillRunsComponentAndGradleFindingsTogether() {
        val permissionRule = recordingRule("permission")
        val componentRule = recordingRule("component")
        val gradleRule = recordingRule("gradle")
        val evaluator = ReleaseRuleEvaluator(
            permissionRules = listOf(permissionRule),
            componentRules = listOf(componentRule),
            gradleRules = listOf(gradleRule)
        )

        val findings = evaluator.evaluate(
            projectPath = Path.of("."),
            currentPermissions = listOf("perm-1"),
            baselinePermissions = null,
            manifestComponents = listOf(component()),
            gradleAndroidConfigs = listOf(config())
        )

        assertEquals(listOf("component", "gradle"), findings.map(ReleaseFinding::ruleId))
        assertFalse(permissionRule.wasCalled)
        assertTrue(componentRule.wasCalled)
        assertTrue(gradleRule.wasCalled)
    }

    private fun findingRule(ruleId: String, expectedPermission: String): ReleaseRule =
        ReleaseRule { context ->
            if (expectedPermission in context.permissions || context.manifestComponents.isNotEmpty() || context.gradleAndroidConfigs.isNotEmpty()) {
                listOf(finding(ruleId))
            } else {
                emptyList()
            }
        }

    private fun recordingRule(ruleId: String): RecordingRule =
        RecordingRule(ruleId)

    private fun finding(ruleId: String): ReleaseFinding =
        ReleaseFinding(
            ruleId = ruleId,
            severity = ReleaseFindingSeverity.MEDIUM,
            title = ruleId,
            description = ruleId,
            evidence = emptyList(),
            recommendation = ruleId
        )

    private fun component(): ManifestComponent =
        ManifestComponent(
            manifestFile = Path.of("src/main/AndroidManifest.xml"),
            type = ManifestComponentType.ACTIVITY,
            name = ".Component",
            exported = true,
            hasIntentFilter = false
        )

    private fun config(): GradleAndroidConfig =
        GradleAndroidConfig(
            gradleFile = Path.of("app/build.gradle.kts"),
            androidPluginType = AndroidPluginType.APPLICATION,
            hasAndroidPlugin = true,
            applicationId = "com.example.app",
            namespace = "com.example.app",
            compileSdk = 35,
            minSdk = 26,
            targetSdk = 35,
            versionCode = 1,
            versionName = "1.0.0",
            minifyEnabled = true,
            shrinkResources = null,
            releaseMinifyEnabled = false,
            releaseShrinkResources = null
        )

    private class RecordingRule(
        private val ruleId: String
    ) : ReleaseRule {
        var wasCalled: Boolean = false
            private set

        override fun evaluate(context: ReleaseRuleContext): List<ReleaseFinding> {
            wasCalled = true
            return listOf(
                ReleaseFinding(
                    ruleId = ruleId,
                    severity = ReleaseFindingSeverity.MEDIUM,
                    title = ruleId,
                    description = ruleId,
                    evidence = emptyList(),
                    recommendation = ruleId
                )
            )
        }
    }
}
