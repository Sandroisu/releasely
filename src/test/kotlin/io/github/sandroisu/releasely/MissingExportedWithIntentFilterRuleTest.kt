package io.github.sandroisu.releasely

import io.github.sandroisu.releasely.rules.MissingExportedWithIntentFilterRule
import io.github.sandroisu.releasely.rules.ReleaseRule
import io.github.sandroisu.releasely.rules.ReleaseRuleContext
import io.github.sandroisu.releasely.scanners.manifrest.ManifestComponent
import io.github.sandroisu.releasely.scanners.manifrest.ManifestComponentType
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MissingExportedWithIntentFilterRuleTest {

    private val rule = MissingExportedWithIntentFilterRule()

    @Test
    fun returnsMediumFindingForActivityWithIntentFilterAndMissingExported() {
        val finding = rule.evaluate(contextWith(component(ManifestComponentType.ACTIVITY, null, true))).single()

        assertEquals("manifest.component.missing_exported.activity", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.MEDIUM, finding.severity)
    }

    @Test
    fun returnsMediumFindingForActivityAliasWithIntentFilterAndMissingExported() {
        val finding = rule.evaluate(contextWith(component(ManifestComponentType.ACTIVITY_ALIAS, null, true))).single()

        assertEquals("manifest.component.missing_exported.activity_alias", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.MEDIUM, finding.severity)
    }

    @Test
    fun returnsHighFindingForServiceWithIntentFilterAndMissingExported() {
        val finding = rule.evaluate(contextWith(component(ManifestComponentType.SERVICE, null, true))).single()

        assertEquals("manifest.component.missing_exported.service", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.HIGH, finding.severity)
    }

    @Test
    fun returnsHighFindingForReceiverWithIntentFilterAndMissingExported() {
        val finding = rule.evaluate(contextWith(component(ManifestComponentType.RECEIVER, null, true))).single()

        assertEquals("manifest.component.missing_exported.receiver", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.HIGH, finding.severity)
    }

    @Test
    fun returnsHighFindingForProviderWithIntentFilterAndMissingExported() {
        val finding = rule.evaluate(contextWith(component(ManifestComponentType.PROVIDER, null, true))).single()

        assertEquals("manifest.component.missing_exported.provider", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.HIGH, finding.severity)
    }

    @Test
    fun ignoresComponentWithoutIntentFilter() {
        val findings = rule.evaluate(contextWith(component(ManifestComponentType.ACTIVITY, null, false)))

        assertTrue(findings.isEmpty())
    }

    @Test
    fun ignoresComponentWithExportedTrue() {
        val findings = rule.evaluate(contextWith(component(ManifestComponentType.SERVICE, true, true)))

        assertTrue(findings.isEmpty())
    }

    @Test
    fun ignoresComponentWithExportedFalse() {
        val findings = rule.evaluate(contextWith(component(ManifestComponentType.RECEIVER, false, true)))

        assertTrue(findings.isEmpty())
    }

    @Test
    fun canBeUsedThroughReleaseRuleInterface() {
        val releaseRule: ReleaseRule = rule

        val finding = releaseRule.evaluate(contextWith(component(ManifestComponentType.PROVIDER, null, true))).single()

        assertEquals("manifest.component.missing_exported.provider", finding.ruleId)
        assertEquals(ReleaseFindingSeverity.HIGH, finding.severity)
    }

    private fun contextWith(vararg components: ManifestComponent): ReleaseRuleContext =
        ReleaseRuleContext(
            projectPath = Path.of("."),
            permissions = emptyList(),
            manifestComponents = components.toList()
        )

    private fun component(
        type: ManifestComponentType,
        exported: Boolean?,
        hasIntentFilter: Boolean
    ): ManifestComponent =
        ManifestComponent(
            manifestFile = Path.of("src/main/AndroidManifest.xml"),
            type = type,
            name = ".Component",
            exported = exported,
            hasIntentFilter = hasIntentFilter
        )
}
