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

        echo("Permissions: ${permissionScanResult.permissions.size}")
        permissionScanResult.permissions.forEach { permission ->
            echo("- $permission")
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
}


fun main(args: Array<String>) {
    ReleaselyCommand().subcommands(ScanCommand()).main(args)
}