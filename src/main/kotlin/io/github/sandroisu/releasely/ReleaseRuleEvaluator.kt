package io.github.sandroisu.releasely

import java.nio.file.Path

class ReleaseRuleEvaluator(
    private val permissionRules: List<ReleaseRule> = listOf(
        ManifestPermissionRiskRule(),
        DangerousPermissionRule()
    ),
    private val componentRules: List<ReleaseRule> = listOf(
        ExportedComponentRule(),
        MissingExportedWithIntentFilterRule()
    ),
    private val gradleRules: List<ReleaseRule> = listOf(
        MinifyDisabledReleaseRule(),
        DebuggableReleaseRule()
    )
) {

    fun evaluate(
        projectPath: Path,
        currentPermissions: List<String>,
        baselinePermissions: List<String>?,
        manifestComponents: List<ManifestComponent>,
        gradleAndroidConfigs: List<GradleAndroidConfig>
    ): List<ReleaseFinding> {
        val permissionFindings = if (baselinePermissions != null) {
            val newPermissions = currentPermissions.filterNot(baselinePermissions.toSet()::contains)
            evaluateRules(
                rules = permissionRules,
                context = ReleaseRuleContext(
                    projectPath = projectPath,
                    permissions = newPermissions,
                    manifestComponents = emptyList(),
                    gradleAndroidConfigs = emptyList()
                )
            )
        } else {
            emptyList()
        }

        val componentFindings = evaluateRules(
            rules = componentRules,
            context = ReleaseRuleContext(
                projectPath = projectPath,
                permissions = emptyList(),
                manifestComponents = manifestComponents,
                gradleAndroidConfigs = emptyList()
            )
        )

        val gradleFindings = evaluateRules(
            rules = gradleRules,
            context = ReleaseRuleContext(
                projectPath = projectPath,
                permissions = emptyList(),
                manifestComponents = emptyList(),
                gradleAndroidConfigs = gradleAndroidConfigs
            )
        )

        return permissionFindings + componentFindings + gradleFindings
    }

    private fun evaluateRules(
        rules: List<ReleaseRule>,
        context: ReleaseRuleContext
    ): List<ReleaseFinding> =
        rules.flatMap { rule -> rule.evaluate(context) }
}
