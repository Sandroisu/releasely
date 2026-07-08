package io.github.sandroisu.releasely

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class MarkdownFindingReportWriterTest {

    private val writer = MarkdownFindingReportWriter()

    @Test
    fun writesEmptyFindingsReport() {
        val report = writer.write(emptyList())

        assertContains(report, "# Releasely Report")
        assertContains(report, "Findings: 0")
        assertContains(report, "## Findings by Severity")
        assertContains(report, "## Findings by Rule")
        assertContains(report, "## Findings")
    }

    @Test
    fun writesFindingWithLocationPath() {
        val report = writer.write(
            listOf(
                finding(
                    ruleId = "manifest.component.exported.activity",
                    severity = ReleaseFindingSeverity.MEDIUM,
                    title = "Exported manifest component detected",
                    locationPath = "app/src/main/AndroidManifest.xml"
                )
            )
        )

        assertContains(report, "### [MEDIUM] Exported manifest component detected")
        assertContains(report, "- Location: app/src/main/AndroidManifest.xml")
    }

    @Test
    fun writesFindingWithoutLocationPath() {
        val report = writer.write(
            listOf(
                finding(
                    ruleId = "manifest.permission.dangerous.camera",
                    severity = ReleaseFindingSeverity.HIGH,
                    title = "Dangerous permission detected",
                    locationPath = null
                )
            )
        )

        assertContains(report, "### [HIGH] Dangerous permission detected")
        assertEquals(false, report.contains("- Location:"))
    }

    @Test
    fun groupsFindingsBySeverity() {
        val report = writer.write(
            listOf(
                finding("rule.high", ReleaseFindingSeverity.HIGH, "High"),
                finding("rule.medium.one", ReleaseFindingSeverity.MEDIUM, "Medium one"),
                finding("rule.medium.two", ReleaseFindingSeverity.MEDIUM, "Medium two")
            )
        )

        assertContains(report, "- HIGH: 1")
        assertContains(report, "- MEDIUM: 2")
    }

    @Test
    fun groupsFindingsByRule() {
        val report = writer.write(
            listOf(
                finding("b.rule", ReleaseFindingSeverity.MEDIUM, "Second"),
                finding("a.rule", ReleaseFindingSeverity.MEDIUM, "First"),
                finding("b.rule", ReleaseFindingSeverity.HIGH, "Third")
            )
        )

        assertContains(report, "- a.rule: 1")
        assertContains(report, "- b.rule: 2")
    }

    private fun finding(
        ruleId: String,
        severity: ReleaseFindingSeverity,
        title: String,
        locationPath: String? = null
    ): ReleaseFinding =
        ReleaseFinding(
            ruleId = ruleId,
            severity = severity,
            title = title,
            description = "Description for $ruleId",
            evidence = listOf("Evidence for $ruleId"),
            recommendation = "Recommendation for $ruleId",
            locationPath = locationPath
        )
}
