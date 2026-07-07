package io.github.sandroisu.releasely

class MissingExportedWithIntentFilterRule : ReleaseRule {

    override fun evaluate(context: ReleaseRuleContext): List<ReleaseFinding> =
        context.manifestComponents
            .filter { component ->
                component.hasIntentFilter && component.exported == null
            }
            .map(::findingFor)

    private fun findingFor(component: ManifestComponent): ReleaseFinding =
        ReleaseFinding(
            ruleId = "manifest.component.missing_exported.${component.type.ruleIdSuffix()}",
            severity = component.type.severity(),
            title = "Missing android:exported on component with intent-filter",
            description = "Android manifest declares a component with an intent-filter but without explicit android:exported. This can break Android 12+ compatibility when targetSdk is 31 or higher.",
            evidence = buildList {
                add("Component type: ${component.type.descriptionName()}")
                component.name?.let { componentName ->
                    add("Component name: $componentName")
                }
                add("Manifest file: ${component.manifestFile}")
            },
            recommendation = "Declare android:exported explicitly. Use android:exported=\"true\" only if the component must be accessible from other apps; otherwise use android:exported=\"false\"."
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
