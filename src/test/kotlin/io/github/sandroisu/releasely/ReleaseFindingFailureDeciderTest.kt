package io.github.sandroisu.releasely

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReleaseFindingFailureDeciderTest {

    private val decider = ReleaseFindingFailureDecider()

    @Test
    fun nullThresholdDoesNotFail() {
        assertFalse(decider.shouldFail(findings(ReleaseFindingSeverity.HIGH), null))
    }

    @Test
    fun highThresholdFailsOnHigh() {
        assertTrue(decider.shouldFail(findings(ReleaseFindingSeverity.HIGH), ReleaseFindingSeverity.HIGH))
    }

    @Test
    fun highThresholdDoesNotFailOnMedium() {
        assertFalse(decider.shouldFail(findings(ReleaseFindingSeverity.MEDIUM), ReleaseFindingSeverity.HIGH))
    }

    @Test
    fun mediumThresholdFailsOnMedium() {
        assertTrue(decider.shouldFail(findings(ReleaseFindingSeverity.MEDIUM), ReleaseFindingSeverity.MEDIUM))
    }

    @Test
    fun lowThresholdFailsOnMedium() {
        assertTrue(decider.shouldFail(findings(ReleaseFindingSeverity.MEDIUM), ReleaseFindingSeverity.LOW))
    }

    @Test
    fun infoThresholdFailsOnLow() {
        assertTrue(decider.shouldFail(findings(ReleaseFindingSeverity.LOW), ReleaseFindingSeverity.INFO))
    }

    @Test
    fun emptyFindingsDoNotFail() {
        assertFalse(decider.shouldFail(emptyList(), ReleaseFindingSeverity.INFO))
    }

    private fun findings(vararg severities: ReleaseFindingSeverity): List<ReleaseFinding> =
        severities.mapIndexed { index, severity ->
            ReleaseFinding(
                ruleId = "rule.$index",
                severity = severity,
                title = "Title $index",
                description = "Description $index",
                evidence = emptyList(),
                recommendation = "Recommendation $index"
            )
        }
}
