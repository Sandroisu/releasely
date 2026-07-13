package io.github.sandroisu.releasely

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import java.nio.file.Path

class ReleaselyCommand : CliktCommand(name = "releasely") {
    override fun help(context: Context): String =
        "Deterministic Android release risk auditor"

    override fun run() = Unit
}

class ScanCommand(
    private val scanOrchestrator: ScanOrchestrator = ScanOrchestrator()
) : CliktCommand(name = "scan") {
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
        help = "Write a Markdown findings report to the given file path"
    )

    private val jsonReportPath: String? by option(
        "--json-report",
        help = "Write a JSON findings report to the given file path"
    )

    private val failOnSeverityName: String? by option(
        "--fail-on",
        help = "Fail with non-zero exit code when findings reach this severity threshold. Allowed values: INFO, LOW, MEDIUM, HIGH. Example: MEDIUM fails on MEDIUM and HIGH findings."
    )

    override fun run() {
        val result = scanOrchestrator.execute(
            ScanRequest(
                projectPath = Path.of(path).toAbsolutePath().normalize(),
                baseRef = baseRef,
                markdownReportPath = markdownReportPath,
                jsonReportPath = jsonReportPath,
                failOnSeverity = parseFailOnSeverity(failOnSeverityName)
            )
        )

        echo(result.standardOutput)
        if (result.shouldFail) {
            throw ProgramResult(1)
        }
    }

    private fun parseFailOnSeverity(value: String?): ReleaseFindingSeverity? {
        if (value == null) {
            return null
        }

        return ReleaseFindingSeverity.entries.firstOrNull { severity ->
            severity.name == value.uppercase()
        } ?: run {
            echo("Invalid --fail-on severity: $value")
            echo("Allowed values: INFO, LOW, MEDIUM, HIGH")
            throw ProgramResult(2)
        }
    }
}

fun main(args: Array<String>) {
    ReleaselyCommand().subcommands(ScanCommand()).main(args)
}
