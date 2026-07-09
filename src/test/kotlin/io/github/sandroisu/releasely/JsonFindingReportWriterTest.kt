package io.github.sandroisu.releasely

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class JsonFindingReportWriterTest {

    private val writer = JsonFindingReportWriter()

    @Test
    fun writesEmptyFindingsReport() {
        val report = writer.write(emptyList())

        assertEquals(
            """
            {
              "findingsCount": 0,
              "findingsBySeverity": {},
              "findingsByRule": {},
              "findings": [
              ]
            }
            """.trimIndent(),
            report
        )
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

        assertContains(report, "\"locationPath\": \"app/src/main/AndroidManifest.xml\"")
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

        assertContains(report, "\"locationPath\": null")
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

        assertContains(report, "\"findingsBySeverity\": {\"HIGH\": 1, \"MEDIUM\": 2}")
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

        assertContains(report, "\"findingsByRule\": {\"a.rule\": 1, \"b.rule\": 2}")
    }

    @Test
    fun escapesStrings() {
        val report = writer.write(
            listOf(
                ReleaseFinding(
                    ruleId = "rule\"id\\with",
                    severity = ReleaseFindingSeverity.HIGH,
                    title = "Title\nline",
                    description = "Desc\rline\tvalue",
                    evidence = listOf("Evidence \"quoted\"", "Path\\value"),
                    recommendation = "Use\tcare\nplease",
                    locationPath = "app\\src\\main\\AndroidManifest.xml"
                )
            )
        )

        assertContains(report, "\"ruleId\": \"rule\\\"id\\\\with\"")
        assertContains(report, "\"title\": \"Title\\nline\"")
        assertContains(report, "\"description\": \"Desc\\rline\\tvalue\"")
        assertContains(report, "\"locationPath\": \"app\\\\src\\\\main\\\\AndroidManifest.xml\"")
        assertContains(report, "\"evidence\": [\"Evidence \\\"quoted\\\"\", \"Path\\\\value\"]")
        assertContains(report, "\"recommendation\": \"Use\\tcare\\nplease\"")
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
