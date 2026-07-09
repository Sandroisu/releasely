package io.github.sandroisu.releasely

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import java.nio.file.Path


class ReleaselyCommand : CliktCommand(name = "releasely") {
    override fun help(context: Context): String =
        "AI release auditor for mobile apps"

    override fun run() = Unit
}

class ScanCommand : CliktCommand(name = "scan") {
    override fun help(context: Context): String =
        "Scan project for release risks"

    private val path: String by option(
        "--path",
        "-p",
        help = "Path to project directory"
    ).default(".")

    private val baseRef: String by option(
        "--base-ref",
        help = "Git ref used as the permission baseline"
    ).default("HEAD")

    private val markdownReportPath: String? by option(
        "--markdown-report",
        help = "Path to write Markdown findings report"
    )

    private val jsonReportPath: String? by option(
        "--json-report",
        help = "Path to write JSON findings report"
    )

    override fun run() {
        val result = ProjectDetector().detect(java.nio.file.Path.of(path).toAbsolutePath().normalize())

        echo("Releasely scan started")
        echo("Path: ${result.path}")
        echo("Project exists: ${yesOrNo(result.exists)}")
        echo("Directory: ${yesOrNo(result.isDirectory)}")
        echo("Gradle project: ${yesOrNo(result.isGradleProject)}")
        echo("Android project: ${yesOrNo(result.isAndroidProject)}")
        if (result.androidEvidence.isNotEmpty()) {
            echo("Android evidence: ${result.androidEvidence.size}")
            result.androidEvidence.take(10).forEach {
                echo("- ${result.path.relativize(it)}")
            }
            if (result.androidEvidence.size > 10) {
                echo("- ... and ${result.androidEvidence.size - 10} more")
            }
        }
        echo("Manifest files: ${result.manifestFiles.size}")
        result.manifestFiles.take(20).forEach {
            echo("- ${result.path.relativize(it)}")
        }
        if (result.manifestFiles.size > 20) {
            echo("- ... and ${result.manifestFiles.size - 20} more")
        }
        val gradleConfigScanResult = GradleAndroidConfigScanner().scan(result.gradleFiles)
        echo("Gradle Android configs: ${gradleConfigScanResult.configs.size}")
        echo("- Android application modules: ${gradleConfigScanResult.count(AndroidPluginType.APPLICATION)}")
        echo("- Android library modules: ${gradleConfigScanResult.count(AndroidPluginType.LIBRARY)}")
        echo("- Android dynamic feature modules: ${gradleConfigScanResult.count(AndroidPluginType.DYNAMIC_FEATURE)}")
        echo("- Android test modules: ${gradleConfigScanResult.count(AndroidPluginType.TEST)}")
        echo("- unknown Android modules: ${gradleConfigScanResult.count(AndroidPluginType.UNKNOWN_ANDROID)}")
        echo("- configs with namespace: ${gradleConfigScanResult.count { config -> config.namespace != null }}")
        echo("- configs with compileSdk: ${gradleConfigScanResult.count { config -> config.compileSdk != null }}")
        echo("- configs with minSdk: ${gradleConfigScanResult.count { config -> config.minSdk != null }}")
        echo("- configs with targetSdk: ${gradleConfigScanResult.count { config -> config.targetSdk != null }}")
        echo("- configs with versionCode: ${gradleConfigScanResult.count { config -> config.versionCode != null }}")
        echo("- configs with minifyEnabled: ${gradleConfigScanResult.count { config -> config.minifyEnabled != null }}")
        echo("- configs with shrinkResources: ${gradleConfigScanResult.count { config -> config.shrinkResources != null }}")
        echo("- configs with release minifyEnabled: ${gradleConfigScanResult.count { config -> config.releaseMinifyEnabled != null }}")
        echo("- configs with release shrinkResources: ${gradleConfigScanResult.count { config -> config.releaseShrinkResources != null }}")
        if (gradleConfigScanResult.failedGradleFiles.isNotEmpty()) {
            echo("Failed Gradle files: ${gradleConfigScanResult.failedGradleFiles.size}")
            gradleConfigScanResult.failedGradleFiles.take(10).forEach { gradleFile ->
                echo("- ${result.path.relativize(gradleFile)}")
            }
        }
        val permissionScanResult = ManifestPermissionScanner().scan(result.manifestFiles)
        val componentScanResult = ManifestComponentScanner().scan(result.manifestFiles)

        echo("Manifest components: ${componentScanResult.components.size}")
        echo("- activities: ${componentScanResult.count(ManifestComponentType.ACTIVITY)}")
        echo("- activity aliases: ${componentScanResult.count(ManifestComponentType.ACTIVITY_ALIAS)}")
        echo("- services: ${componentScanResult.count(ManifestComponentType.SERVICE)}")
        echo("- receivers: ${componentScanResult.count(ManifestComponentType.RECEIVER)}")
        echo("- providers: ${componentScanResult.count(ManifestComponentType.PROVIDER)}")
        if (componentScanResult.failedManifestFiles.isNotEmpty()) {
            echo("Failed component manifest files: ${componentScanResult.failedManifestFiles.size}")
            componentScanResult.failedManifestFiles.take(10).forEach { manifestFile ->
                echo("- ${result.path.relativize(manifestFile)}")
            }
        }

        echo("Permissions: ${permissionScanResult.permissions.size}")
        permissionScanResult.permissions.forEach { permission ->
            echo("- $permission")
        }

        val baselineResult = GitPermissionBaselineResolver().resolve(
            projectRoot = result.path,
            currentManifestFiles = result.manifestFiles,
            baseRef = baseRef
        )
        val findings = if (baselineResult.failureReason == null) {
            echo("Permission baseline: $baseRef")
            val baselinePermissions = baselineResult.permissions.toSet()
            val newPermissions = permissionScanResult.permissions.filterNot(baselinePermissions::contains)
            echo("New permissions since baseline: ${newPermissions.size}")
            echo("Baseline permissions: ${baselineResult.permissions.size}")
            if (newPermissions.isNotEmpty()) {
                echo("New permissions:")
                newPermissions.forEach { permission ->
                    echo("- $permission")
                }
            }
            val releaseRuleContext = ReleaseRuleContext(
                projectPath = result.path,
                permissions = newPermissions,
                manifestComponents = componentScanResult.components,
                gradleAndroidConfigs = gradleConfigScanResult.configs
            )
            val releaseRules: List<ReleaseRule> = listOf(
                ManifestPermissionRiskRule(),
                DangerousPermissionRule(),
                ExportedComponentRule(),
                MissingExportedWithIntentFilterRule(),
                MinifyDisabledReleaseRule()
            )

            releaseRules.flatMap { releaseRule ->
                releaseRule.evaluate(releaseRuleContext)
            }
        } else {
            echo("Permission baseline unavailable: ${baselineResult.failureReason}")
            emptyList()
        }
        echo("Finding scope:")
        echo("- Permission rules: new permissions since baseline")
        echo("- Manifest component rules: all manifest components")
        echo("- Gradle rules: all detected Android Gradle configs")
        echo("Findings: ${findings.size}")
        echoFindingsBySeverity(findings)
        echoFindingsByRule(findings)
        findings.forEachIndexed { findingIndex, finding ->
            echo("[${finding.severity}] ${finding.title}")
            echo("Rule: ${finding.ruleId}")
            echo("Description: ${finding.description}")
            echo("Evidence:")
            finding.evidence.forEach { evidence ->
                echo("- $evidence")
            }
            echo("Recommendation:")
            echo(finding.recommendation)
            if (findingIndex < findings.lastIndex) {
                echo()
            }
        }

        val reportFileWriter = ReportFileWriter()

        markdownReportPath?.let { reportPath ->
            val markdownReport = MarkdownFindingReportWriter().write(findings)
            reportFileWriter.write(Path.of(reportPath), markdownReport)
            echo("Markdown report written: $reportPath")
        }

        jsonReportPath?.let { reportPath ->
            val jsonReport = JsonFindingReportWriter().write(findings)
            reportFileWriter.write(Path.of(reportPath), jsonReport)
            echo("JSON report written: $reportPath")
        }

        if (permissionScanResult.failedManifestFiles.isNotEmpty()) {
            echo("Failed manifest files: ${permissionScanResult.failedManifestFiles.size}")
            permissionScanResult.failedManifestFiles.take(10).forEach { manifestFile ->
                echo("- ${result.path.relativize(manifestFile)}")
            }
        }
    }

    private fun yesOrNo(value: Boolean): String =
        if (value) {
            "yes"
        } else {
            "no"
        }

    private fun echoFindingsBySeverity(findings: List<ReleaseFinding>) {
        if (findings.isEmpty()) {
            return
        }

        val findingsBySeverity = findings.groupingBy(ReleaseFinding::severity).eachCount()
        echo("Findings by severity:")
        listOf(
            ReleaseFindingSeverity.HIGH,
            ReleaseFindingSeverity.MEDIUM,
            ReleaseFindingSeverity.LOW,
            ReleaseFindingSeverity.INFO
        ).forEach { severity ->
            val count = findingsBySeverity[severity] ?: return@forEach
            echo("- $severity: $count")
        }
    }

    private fun echoFindingsByRule(findings: List<ReleaseFinding>) {
        if (findings.isEmpty()) {
            return
        }

        echo("Findings by rule:")
        findings
            .groupingBy(ReleaseFinding::ruleId)
            .eachCount()
            .toSortedMap()
            .forEach { (ruleId, count) ->
                echo("- $ruleId: $count")
            }
    }

    private fun ManifestComponentScanResult.count(type: ManifestComponentType): Int =
        components.count { component -> component.type == type }

    private fun GradleAndroidConfigScanResult.count(pluginType: AndroidPluginType): Int =
        configs.count { config -> config.androidPluginType == pluginType }

    private fun GradleAndroidConfigScanResult.count(predicate: (GradleAndroidConfig) -> Boolean): Int =
        configs.count(predicate)
}


fun main(args: Array<String>) {
    ReleaselyCommand().subcommands(ScanCommand()).main(args)
}
