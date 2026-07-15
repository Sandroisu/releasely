package io.github.sandroisu.releasely.rules

import io.github.sandroisu.releasely.*

class ExportedComponentRule : ReleaseRule {

    override fun evaluate(context: ReleaseRuleContext): List<ReleaseFinding> =
        context.manifestComponents
            .filter { component -> component.exported == true }
            .map(::findingFor)

    private fun findingFor(component: ManifestComponent): ReleaseFinding =
        ReleaseFinding(
            ruleId = "manifest.component.exported.${component.type.ruleIdSuffix()}",
            severity = component.type.severity(),
            title = "Exported manifest component detected",
            description = "Android manifest declares an exported ${component.type.descriptionName()}. Exported components can be invoked by other apps and should be verified before release.",
            evidence = buildList {
                add("Component type: ${component.type.descriptionName()}")
                component.name?.let { componentName ->
                    add("Component name: $componentName")
                }
                add("Manifest file: ${component.manifestFile}")
            },
            recommendation = "Verify that this component must be exported, has the expected intent filters, is protected when needed, and is covered by release QA.",
            locationPath = component.manifestFile.toString()
        )

    private fun ManifestComponentType.ruleIdSuffix(): String =
        when (this) {
            ManifestComponentType.ACTIVITY -> "activity"
            ManifestComponentType.ACTIVITY_ALIAS -> "activity_alias"
            ManifestComponentType.SERVICE -> "service"
            ManifestComponentType.RECEIVER -> "receiver"
            ManifestComponentType.PROVIDER -> "provider"
        }

    private fun ManifestComponentType.severity(): ReleaseFindingSeverity =
        when (this) {
            ManifestComponentType.PROVIDER,
            ManifestComponentType.SERVICE,
            ManifestComponentType.RECEIVER -> ReleaseFindingSeverity.HIGH

            ManifestComponentType.ACTIVITY,
            ManifestComponentType.ACTIVITY_ALIAS -> ReleaseFindingSeverity.MEDIUM
        }

    private fun ManifestComponentType.descriptionName(): String =
        when (this) {
            ManifestComponentType.ACTIVITY -> "activity"
            ManifestComponentType.ACTIVITY_ALIAS -> "activity alias"
            ManifestComponentType.SERVICE -> "service"
            ManifestComponentType.RECEIVER -> "receiver"
            ManifestComponentType.PROVIDER -> "provider"
        }
}
