package io.github.sandroisu.releasely

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DangerousPermissionRuleTest {

    private val rule = DangerousPermissionRule()

    @Test
    fun returnsHighFindingForCameraPermission() {
        val finding = rule.evaluate(contextWith("android.permission.CAMERA")).single()

        assertEquals("manifest.permission.dangerous.camera", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.HIGH, finding.severity)
    }

    @Test
    fun returnsHighFindingForFineLocationPermission() {
        val finding = rule.evaluate(contextWith("android.permission.ACCESS_FINE_LOCATION")).single()

        assertEquals("manifest.permission.dangerous.access_fine_location", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.HIGH, finding.severity)
    }

    @Test
    fun returnsMediumFindingForPostNotificationsPermission() {
        val finding = rule.evaluate(contextWith("android.permission.POST_NOTIFICATIONS")).single()

        assertEquals("manifest.permission.dangerous.post_notifications", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.MEDIUM, finding.severity)
    }

    @Test
    fun returnsEmptyListForNonDangerousPermission() {
        val findings = rule.evaluate(contextWith("android.permission.INTERNET"))

        assertTrue(findings.isEmpty())
    }

    @Test
    fun canBeUsedThroughReleaseRuleInterface() {
        val releaseRule: ReleaseRule = rule

        val finding = releaseRule.evaluate(contextWith("android.permission.BLUETOOTH_SCAN")).single()

        assertEquals("manifest.permission.dangerous.bluetooth_scan", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.MEDIUM, finding.severity)
    }

    private fun contextWith(vararg permissions: String): ReleaseRuleContext =
        ReleaseRuleContext(
            projectPath = Path.of("."),
            permissions = permissions.toList(),
            manifestComponents = emptyList()
        )
}
