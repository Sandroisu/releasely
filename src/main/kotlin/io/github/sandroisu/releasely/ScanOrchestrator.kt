package io.github.sandroisu.releasely

import io.github.sandroisu.releasely.rules.ReleaseRuleEvaluator
import io.github.sandroisu.releasely.scanners.manifrest.*
import java.nio.file.Path

data class ScanRequest(
    val projectPath: Path,
    val baseRef: String,
    val markdownReportPath: String?,
    val jsonReportPath: String?,
    val failOnSeverity: ReleaseFindingSeverity?
)

data class GeneratedScanReport(
    val path: Path,
    val content: String
)

data class ScanExecutionResult(
    val findings: List<ReleaseFinding>,
    val permissionBaseline: PermissionBaselineResult,
    val standardOutput: String,
    val markdownReport: GeneratedScanReport?,
    val jsonReport: GeneratedScanReport?,
    val shouldFail: Boolean
)

class ScanOrchestrator {

    fun execute(request: ScanRequest): ScanExecutionResult {
        val output = mutableListOf<String>()
        val project = ProjectDetector().detect(request.projectPath)
        renderProjectSummary(project, output)

        val gradleScan = GradleAndroidConfigScanner().scan(project.gradleFiles)
        renderGradleSummary(project, gradleScan, output)

        val permissionScan = ManifestPermissionScanner().scan(project.manifestFiles)
        val componentScan = ManifestComponentScanner().scan(project.manifestFiles)
        renderComponentSummary(project, componentScan, output)
        renderPermissionSummary(permissionScan, output)

        val permissionBaseline = GitPermissionBaselineResolver().resolve(
            projectRoot = project.path,
            currentManifestFiles = project.manifestFiles,
            baseRef = request.baseRef
        )
        val baselinePermissions = renderPermissionBaseline(
            baseRef = request.baseRef,
            permissionScan = permissionScan,
            permissionBaseline = permissionBaseline,
            output = output
        )

        val findings = ReleaseRuleEvaluator().evaluate(
            projectPath = project.path,
            currentPermissions = permissionScan.permissions,
            baselinePermissions = baselinePermissions,
            manifestComponents = componentScan.components,
            gradleAndroidConfigs = gradleScan.configs
        )
        renderFindings(findings, output)

        val reportFileWriter = ReportFileWriter()
        val markdownReport = request.markdownReportPath?.let { reportPath ->
            val report = GeneratedScanReport(
                path = Path.of(reportPath),
                content = MarkdownFindingReportWriter().write(findings)
            )
            reportFileWriter.write(report.path, report.content)
            output += "Markdown report written: $reportPath"
            report
        }
        val jsonReport = request.jsonReportPath?.let { reportPath ->
            val report = GeneratedScanReport(
                path = Path.of(reportPath),
                content = JsonFindingReportWriter().write(findings)
            )
            reportFileWriter.write(report.path, report.content)
            output += "JSON report written: $reportPath"
            report
        }

        renderFailedPermissionFiles(project, permissionScan, output)

        val shouldFail = ReleaseFindingFailureDecider().shouldFail(findings, request.failOnSeverity)
        if (shouldFail) {
            output += "Releasely failed because findings reached threshold: ${request.failOnSeverity?.name}"
        }

        return ScanExecutionResult(
            findings = findings,
            permissionBaseline = permissionBaseline,
            standardOutput = output.joinToString("\n"),
            markdownReport = markdownReport,
            jsonReport = jsonReport,
            shouldFail = shouldFail
        )
    }

    private fun renderProjectSummary(
        project: ProjectDetectionResult,
        output: MutableList<String>
    ) {
        output += "Releasely scan started"
        output += "Path: ${project.path}"
        output += "Project exists: ${yesOrNo(project.exists)}"
        output += "Directory: ${yesOrNo(project.isDirectory)}"
        output += "Gradle project: ${yesOrNo(project.isGradleProject)}"
        output += "Android project: ${yesOrNo(project.isAndroidProject)}"
        if (project.androidEvidence.isNotEmpty()) {
            output += "Android evidence: ${project.androidEvidence.size}"
            project.androidEvidence.take(10).forEach { evidence ->
                output += "- ${project.path.relativize(evidence)}"
            }
            if (project.androidEvidence.size > 10) {
                output += "- ... and ${project.androidEvidence.size - 10} more"
            }
        }
        output += "Manifest files: ${project.manifestFiles.size}"
        project.manifestFiles.take(20).forEach { manifestFile ->
            output += "- ${project.path.relativize(manifestFile)}"
        }
        if (project.manifestFiles.size > 20) {
            output += "- ... and ${project.manifestFiles.size - 20} more"
        }
    }

    private fun renderGradleSummary(
        project: ProjectDetectionResult,
        gradleScan: GradleAndroidConfigScanResult,
        output: MutableList<String>
    ) {
        output += "Gradle Android configs: ${gradleScan.configs.size}"
        output += "- Android application modules: ${gradleScan.count(AndroidPluginType.APPLICATION)}"
        output += "- Android library modules: ${gradleScan.count(AndroidPluginType.LIBRARY)}"
        output += "- Android dynamic feature modules: ${gradleScan.count(AndroidPluginType.DYNAMIC_FEATURE)}"
        output += "- Android test modules: ${gradleScan.count(AndroidPluginType.TEST)}"
        output += "- unknown Android modules: ${gradleScan.count(AndroidPluginType.UNKNOWN_ANDROID)}"
        output += "- configs with namespace: ${gradleScan.count { config -> config.namespace != null }}"
        output += "- configs with compileSdk: ${gradleScan.count { config -> config.compileSdk != null }}"
        output += "- configs with minSdk: ${gradleScan.count { config -> config.minSdk != null }}"
        output += "- configs with targetSdk: ${gradleScan.count { config -> config.targetSdk != null }}"
        output += "- configs with versionCode: ${gradleScan.count { config -> config.versionCode != null }}"
        output += "- configs with minifyEnabled: ${gradleScan.count { config -> config.minifyEnabled != null }}"
        output += "- configs with shrinkResources: ${gradleScan.count { config -> config.shrinkResources != null }}"
        output += "- configs with release minifyEnabled: ${gradleScan.count { config -> config.releaseMinifyEnabled != null }}"
        output += "- configs with release shrinkResources: ${gradleScan.count { config -> config.releaseShrinkResources != null }}"
        if (gradleScan.failedGradleFiles.isNotEmpty()) {
            output += "Failed Gradle files: ${gradleScan.failedGradleFiles.size}"
            gradleScan.failedGradleFiles.take(10).forEach { gradleFile ->
                output += "- ${project.path.relativize(gradleFile)}"
            }
        }
    }

    private fun renderComponentSummary(
        project: ProjectDetectionResult,
        componentScan: ManifestComponentScanResult,
        output: MutableList<String>
    ) {
        output += "Manifest components: ${componentScan.components.size}"
        output += "- activities: ${componentScan.count(ManifestComponentType.ACTIVITY)}"
        output += "- activity aliases: ${componentScan.count(ManifestComponentType.ACTIVITY_ALIAS)}"
        output += "- services: ${componentScan.count(ManifestComponentType.SERVICE)}"
        output += "- receivers: ${componentScan.count(ManifestComponentType.RECEIVER)}"
        output += "- providers: ${componentScan.count(ManifestComponentType.PROVIDER)}"
        if (componentScan.failedManifestFiles.isNotEmpty()) {
            output += "Failed component manifest files: ${componentScan.failedManifestFiles.size}"
            componentScan.failedManifestFiles.take(10).forEach { manifestFile ->
                output += "- ${project.path.relativize(manifestFile)}"
            }
        }
    }

    private fun renderPermissionSummary(
        permissionScan: ManifestPermissionScanResult,
        output: MutableList<String>
    ) {
        output += "Permissions: ${permissionScan.permissions.size}"
        permissionScan.permissions.forEach { permission ->
            output += "- $permission"
        }
    }

    private fun renderPermissionBaseline(
        baseRef: String,
        permissionScan: ManifestPermissionScanResult,
        permissionBaseline: PermissionBaselineResult,
        output: MutableList<String>
    ): List<String>? =
        if (permissionBaseline.failureReason == null) {
            output += "Permission baseline: $baseRef"
            val newPermissions = permissionScan.permissions.filterNot(permissionBaseline.permissions.toSet()::contains)
            output += "New permissions since baseline: ${newPermissions.size}"
            output += "Baseline permissions: ${permissionBaseline.permissions.size}"
            if (newPermissions.isNotEmpty()) {
                output += "New permissions:"
                newPermissions.forEach { permission ->
                    output += "- $permission"
                }
            }
            permissionBaseline.permissions
        } else {
            output += "Permission baseline unavailable: ${permissionBaseline.failureReason}"
            null
        }

    private fun renderFindings(
        findings: List<ReleaseFinding>,
        output: MutableList<String>
    ) {
        output += "Finding scope:"
        output += "- Permission rules: new permissions since baseline"
        output += "- Manifest component rules: all manifest components"
        output += "- Gradle rules: all detected Android Gradle configs"
        output += "Findings: ${findings.size}"
        renderFindingsBySeverity(findings, output)
        renderFindingsByRule(findings, output)
        findings.forEachIndexed { findingIndex, finding ->
            output += "[${finding.severity}] ${finding.title}"
            output += "Rule: ${finding.ruleId}"
            output += "Description: ${finding.description}"
            output += "Evidence:"
            finding.evidence.forEach { evidence ->
                output += "- $evidence"
            }
            output += "Recommendation:"
            output += finding.recommendation
            if (findingIndex < findings.lastIndex) {
                output += ""
            }
        }
    }

    private fun renderFindingsBySeverity(
        findings: List<ReleaseFinding>,
        output: MutableList<String>
    ) {
        if (findings.isEmpty()) {
            return
        }

        val findingsBySeverity = findings.groupingBy(ReleaseFinding::severity).eachCount()
        output += "Findings by severity:"
        listOf(
            ReleaseFindingSeverity.HIGH,
            ReleaseFindingSeverity.MEDIUM,
            ReleaseFindingSeverity.LOW,
            ReleaseFindingSeverity.INFO
        ).forEach { severity ->
            val count = findingsBySeverity[severity] ?: return@forEach
            output += "- $severity: $count"
        }
    }

    private fun renderFindingsByRule(
        findings: List<ReleaseFinding>,
        output: MutableList<String>
    ) {
        if (findings.isEmpty()) {
            return
        }

        output += "Findings by rule:"
        findings
            .groupingBy(ReleaseFinding::ruleId)
            .eachCount()
            .toSortedMap()
            .forEach { (ruleId, count) ->
                output += "- $ruleId: $count"
            }
    }

    private fun renderFailedPermissionFiles(
        project: ProjectDetectionResult,
        permissionScan: ManifestPermissionScanResult,
        output: MutableList<String>
    ) {
        if (permissionScan.failedManifestFiles.isNotEmpty()) {
            output += "Failed manifest files: ${permissionScan.failedManifestFiles.size}"
            permissionScan.failedManifestFiles.take(10).forEach { manifestFile ->
                output += "- ${project.path.relativize(manifestFile)}"
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

    private fun GradleAndroidConfigScanResult.count(pluginType: AndroidPluginType): Int =
        configs.count { config -> config.androidPluginType == pluginType }

    private fun GradleAndroidConfigScanResult.count(predicate: (GradleAndroidConfig) -> Boolean): Int =
        configs.count(predicate)
}
