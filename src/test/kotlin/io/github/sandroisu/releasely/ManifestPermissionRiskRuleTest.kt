package io.github.sandroisu.releasely

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManifestPermissionRiskRuleTest {

    private val rule = ManifestPermissionRiskRule()

    @Test
    fun `returns a finding when sensitive permission was added`() {
        val findings = rule.evaluate(
            currentPermissions = listOf(
                "android.permission.INTERNET",
                "android.permission.ACCESS_BACKGROUND_LOCATION"
            ),
            baselinePermissions = listOf("android.permission.INTERNET")
        )

        assertEquals(1, findings.size)
        assertEquals("manifest.permission.background_location", findings.single().ruleId)
        assertEquals(ReleaseFindingSeverity.HIGH, findings.single().severity)
    }

    @Test
    fun `does not report a sensitive permission already present in baseline`() {
        val permission = "android.permission.SYSTEM_ALERT_WINDOW"

        val findings = rule.evaluate(
            currentPermissions = listOf(permission),
            baselinePermissions = listOf(permission)
        )

        assertTrue(findings.isEmpty())
    }

    @Test
    fun `returns findings for supported added permissions only`() {
        val findings = rule.evaluate(
            currentPermissions = listOf(
                "android.permission.POST",
                "android.permission.SYSTEM_ALERT_WINDOW",
                "android.permission.ACCESS_BACKGROUND_LOCATION",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
            ),
            baselinePermissions = emptyList()
        )

        assertEquals(4, findings.size)
        assertEquals(
            listOf(
                ReleaseFindingSeverity.HIGH,
                ReleaseFindingSeverity.HIGH,
                ReleaseFindingSeverity.MEDIUM,
                ReleaseFindingSeverity.MEDIUM
            ),
            findings.map(ReleaseFinding::severity)
        )
    }
}
