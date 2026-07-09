package io.github.sandroisu.releasely

class ReleaseFindingFailureDecider {

    fun shouldFail(
        findings: List<ReleaseFinding>,
        threshold: ReleaseFindingSeverity?
    ): Boolean {
        if (threshold == null) {
            return false
        }

        val thresholdRank = threshold.rank()
        return findings.any { finding -> finding.severity.rank() >= thresholdRank }
    }

    private fun ReleaseFindingSeverity.rank(): Int =
        when (this) {
            ReleaseFindingSeverity.INFO -> 0
            ReleaseFindingSeverity.LOW -> 1
            ReleaseFindingSeverity.MEDIUM -> 2
            ReleaseFindingSeverity.HIGH -> 3
        }
}
