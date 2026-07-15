package io.github.sandroisu.releasely

import io.github.sandroisu.releasely.rules.ExportedComponentRule
import io.github.sandroisu.releasely.rules.ReleaseRule
import io.github.sandroisu.releasely.rules.ReleaseRuleContext
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportedComponentRuleTest {

    private val rule = ExportedComponentRule()

    @Test
    fun returnsHighFindingForExportedProvider() {
        val finding = rule.evaluate(contextWith(component(ManifestComponentType.PROVIDER, true))).single()

        assertEquals("manifest.component.exported.provider", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.HIGH, finding.severity)
    }

    @Test
    fun returnsHighFindingForExportedService() {
        val finding = rule.evaluate(contextWith(component(ManifestComponentType.SERVICE, true))).single()

        assertEquals("manifest.component.exported.service", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.HIGH, finding.severity)
    }

    @Test
    fun returnsHighFindingForExportedReceiver() {
        val finding = rule.evaluate(contextWith(component(ManifestComponentType.RECEIVER, true))).single()

        assertEquals("manifest.component.exported.receiver", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.HIGH, finding.severity)
    }

    @Test
    fun returnsMediumFindingForExportedActivity() {
        val finding = rule.evaluate(contextWith(component(ManifestComponentType.ACTIVITY, true))).single()

        assertEquals("manifest.component.exported.activity", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.MEDIUM, finding.severity)
        assertEquals("src/main/AndroidManifest.xml", finding.locationPath?.replace('\\', '/'))
    }

    @Test
    fun returnsMediumFindingForExportedActivityAlias() {
        val finding = rule.evaluate(contextWith(component(ManifestComponentType.ACTIVITY_ALIAS, true))).single()

        assertEquals("manifest.component.exported.activity_alias", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.MEDIUM, finding.severity)
    }

    @Test
    fun ignoresNonExportedComponent() {
        val findings = rule.evaluate(contextWith(component(ManifestComponentType.SERVICE, false)))

        assertTrue(findings.isEmpty())
    }

    @Test
    fun ignoresComponentWithMissingExported() {
        val findings = rule.evaluate(contextWith(component(ManifestComponentType.RECEIVER, null)))

        assertTrue(findings.isEmpty())
    }

    @Test
    fun canBeUsedThroughReleaseRuleInterface() {
        val releaseRule: ReleaseRule = rule

        val finding = releaseRule.evaluate(contextWith(component(ManifestComponentType.ACTIVITY, true))).single()

        assertEquals("manifest.component.exported.activity", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.MEDIUM, finding.severity)
    }

    private fun contextWith(vararg components: ManifestComponent): ReleaseRuleContext =
        ReleaseRuleContext(
            projectPath = Path.of("."),
            permissions = emptyList(),
            manifestComponents = components.toList()
        )

    private fun component(type: ManifestComponentType, exported: Boolean?): ManifestComponent =
        ManifestComponent(
            manifestFile = Path.of("src/main/AndroidManifest.xml"),
            type = type,
            name = ".Component",
            exported = exported,
            hasIntentFilter = false
        )
}
