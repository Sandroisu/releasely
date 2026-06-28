package io.github.sandroisu.releasely

enum class ReleaseFindingSeverity {
    INFO,
    LOW,
    MEDIUM,
    HIGH
}

data class ReleaseFinding(
    val ruleId: String,
    val severity: ReleaseFindingSeverity,
    val title: String,
    val description: String,
    val evidence: List<String>,
    val recommendation: String
)
