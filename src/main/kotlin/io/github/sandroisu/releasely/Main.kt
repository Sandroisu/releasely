package io.github.sandroisu.releasely

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option


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
                manifestComponents = componentScanResult.components
            )
            val releaseRules: List<ReleaseRule> = listOf(
                ManifestPermissionRiskRule(),
                DangerousPermissionRule(),
                ExportedComponentRule(),
                MissingExportedWithIntentFilterRule()
            )

            releaseRules.flatMap { releaseRule ->
                releaseRule.evaluate(releaseRuleContext)
            }
        } else {
            echo("Permission baseline unavailable: ${baselineResult.failureReason}")
            emptyList()
        }
        echo("Findings are based on new permissions since baseline.")
        echo("Findings: ${findings.size}")
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

    private fun ManifestComponentScanResult.count(type: ManifestComponentType): Int =
        components.count { component -> component.type == type }
}


fun main(args: Array<String>) {
    ReleaselyCommand().subcommands(ScanCommand()).main(args)
}
