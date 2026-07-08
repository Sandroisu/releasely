package io.github.sandroisu.releasely

class MarkdownFindingReportWriter {

    fun write(findings: List<ReleaseFinding>): String = buildString {
        appendLine("# Releasely Report")
        appendLine()
        appendLine("Findings: ${findings.size}")
        appendLine()
        appendLine("## Findings by Severity")
        appendGroupedBySeverity(findings)
        appendLine()
        appendLine("## Findings by Rule")
        appendGroupedByRule(findings)
        appendLine()
        appendLine("## Findings")
        appendFindings(findings)
    }.trimEnd()

    private fun StringBuilder.appendGroupedBySeverity(findings: List<ReleaseFinding>) {
        val findingsBySeverity = findings.groupingBy(ReleaseFinding::severity).eachCount()
        listOf(
            ReleaseFindingSeverity.HIGH,
            ReleaseFindingSeverity.MEDIUM,
            ReleaseFindingSeverity.LOW,
            ReleaseFindingSeverity.INFO
        ).forEach { severity ->
            val count = findingsBySeverity[severity] ?: return@forEach
            appendLine("- $severity: $count")
        }
    }

    private fun StringBuilder.appendGroupedByRule(findings: List<ReleaseFinding>) {
        findings
            .groupingBy(ReleaseFinding::ruleId)
            .eachCount()
            .toSortedMap()
            .forEach { (ruleId, count) ->
                appendLine("- $ruleId: $count")
            }
    }

    private fun StringBuilder.appendFindings(findings: List<ReleaseFinding>) {
        findings.forEachIndexed { index, finding ->
            appendLine("### [${finding.severity}] ${finding.title}")
            appendLine()
            appendLine("- Rule: ${finding.ruleId}")
            appendLine("- Description: ${finding.description}")
            finding.locationPath?.let { locationPath ->
                appendLine("- Location: $locationPath")
            }
            if (finding.evidence.isNotEmpty()) {
                appendLine("- Evidence:")
                finding.evidence.forEach { evidence ->
                    appendLine("  - $evidence")
                }
            }
            appendLine("- Recommendation: ${finding.recommendation}")
            if (index < findings.lastIndex) {
                appendLine()
            }
        }
    }
}

