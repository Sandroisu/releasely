package io.github.sandroisu.releasely

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class ManifestPermissionRiskRuleTest {

    private val rule = ManifestPermissionRiskRule()

    @Test
    fun `returns a finding when sensitive permission was added`() {
        val findings = rule.evaluate(
            ReleaseRuleContext(
                projectPath = Path.of("."),
                permissions = listOf(
                    "android.permission.INTERNET",
                    "android.permission.ACCESS_BACKGROUND_LOCATION"
                )
            )
        )

        assertEquals(1, findings.size)
        assertEquals("manifest.permission.background_location", findings.single().ruleId)
        assertEquals(ReleaseFindingSeverity.HIGH, findings.single().severity)
    }

    @Test
    fun `returns a finding through release rule interface`() {
        val releaseRule: ReleaseRule = rule

        val findings = releaseRule.evaluate(
            ReleaseRuleContext(
                projectPath = Path.of("."),
                permissions = listOf("android.permission.SYSTEM_ALERT_WINDOW")
            )
        )

        assertEquals("manifest.permission.system_alert_window", findings.single().ruleId)
    }

    @Test
    fun `returns findings for supported added permissions only`() {
        val findings = rule.evaluate(
            ReleaseRuleContext(
                projectPath = Path.of("."),
                permissions = listOf(
                    "android.permission.POST",
                    "android.permission.SYSTEM_ALERT_WINDOW",
                    "android.permission.ACCESS_BACKGROUND_LOCATION",
                    "android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.WRITE_EXTERNAL_STORAGE",
                    "android.permission.WRITE_EXTERNAL_STORAGE"
                )
            )
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
