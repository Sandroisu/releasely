package io.github.sandroisu.releasely

class JsonFindingReportWriter {

    fun write(findings: List<ReleaseFinding>): String = buildString {
        appendLine("{")
        appendLine("  \"findingsCount\": ${findings.size},")
        appendLine("  \"findingsBySeverity\": ${findingsBySeverityJson(findings)},")
        appendLine("  \"findingsByRule\": ${findingsByRuleJson(findings)},")
        appendLine("  \"findings\": [")
        findings.forEachIndexed { index, finding ->
            appendFinding(finding)
            if (index < findings.lastIndex) {
                appendLine(",")
            } else {
                appendLine()
            }
        }
        appendLine("  ]")
        append("}")
    }

    private fun findingsBySeverityJson(findings: List<ReleaseFinding>): String {
        val findingsBySeverity = findings.groupingBy(ReleaseFinding::severity).eachCount()
        val entries = listOf(
            ReleaseFindingSeverity.HIGH,
            ReleaseFindingSeverity.MEDIUM,
            ReleaseFindingSeverity.LOW,
            ReleaseFindingSeverity.INFO
        ).mapNotNull { severity ->
            findingsBySeverity[severity]?.let { count ->
                "\"${severity.name}\": $count"
            }
        }

        return "{${entries.joinToString(", ")}}"
    }

    private fun findingsByRuleJson(findings: List<ReleaseFinding>): String {
        val entries = findings
            .groupingBy(ReleaseFinding::ruleId)
            .eachCount()
            .toSortedMap()
            .map { (ruleId, count) ->
                "\"${escape(ruleId)}\": $count"
            }

        return "{${entries.joinToString(", ")}}"
    }

    private fun StringBuilder.appendFinding(finding: ReleaseFinding) {
        appendLine("    {")
        appendLine("      \"ruleId\": \"${escape(finding.ruleId)}\",")
        appendLine("      \"severity\": \"${finding.severity.name}\",")
        appendLine("      \"title\": \"${escape(finding.title)}\",")
        appendLine("      \"description\": \"${escape(finding.description)}\",")
        appendLine("      \"locationPath\": ${nullableString(finding.locationPath)},")
        appendLine("      \"evidence\": ${stringArray(finding.evidence)},")
        appendLine("      \"recommendation\": \"${escape(finding.recommendation)}\"")
        append("    }")
    }

    private fun nullableString(value: String?): String =
        value?.let { "\"${escape(it)}\"" } ?: "null"

    private fun stringArray(values: List<String>): String =
        values.joinToString(prefix = "[", postfix = "]") { value ->
            "\"${escape(value)}\""
        }

    private fun escape(value: String): String = buildString {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (character.code < 0x20) {
                        append("\\u%04x".format(character.code))
                    } else {
                        append(character)
                    }
                }
            }
        }
    }
}
