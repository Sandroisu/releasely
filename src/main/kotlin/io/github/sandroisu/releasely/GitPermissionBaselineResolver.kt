package io.github.sandroisu.releasely

import io.github.sandroisu.releasely.scanners.manifrest.ManifestPermissionScanner
import java.nio.file.Path

data class PermissionBaselineResult(
    val permissions: List<String>,
    val failureReason: String? = null
)

class GitPermissionBaselineResolver(
    private val permissionScanner: ManifestPermissionScanner = ManifestPermissionScanner()
) {

    fun resolve(
        projectRoot: Path,
        currentManifestFiles: List<Path>,
        baseRef: String
    ): PermissionBaselineResult {
        val changedFilesResult = runGit(
            projectRoot,
            "diff",
            "--name-only",
            "-z",
            "--no-renames",
            baseRef,
            "--"
        )
        if (changedFilesResult.exitCode != 0) {
            return PermissionBaselineResult(
                permissions = emptyList(),
                failureReason = changedFilesResult.errorText.trim().ifEmpty {
                    "Unable to compare the project with $baseRef"
                }
            )
        }

        val untrackedFilesResult = runGit(
            projectRoot,
            "ls-files",
            "--others",
            "--exclude-standard",
            "-z"
        )
        if (untrackedFilesResult.exitCode != 0) {
            return PermissionBaselineResult(
                permissions = emptyList(),
                failureReason = untrackedFilesResult.errorText.trim().ifEmpty {
                    "Unable to list untracked project files"
                }
            )
        }

        val changedManifestPaths = sequenceOf(
            changedFilesResult.outputText.splitToSequence('\u0000'),
            untrackedFilesResult.outputText.splitToSequence('\u0000')
        )
            .flatten()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .filter { relativePath -> relativePath.endsWith("AndroidManifest.xml") }
            .toSet()

        val unchangedManifestFiles = currentManifestFiles.filterNot { manifestFile ->
            manifestFile.toGitRelativePath(projectRoot) in changedManifestPaths
        }
        val baselinePermissions = permissionScanner
            .scan(unchangedManifestFiles)
            .permissions
            .toMutableSet()

        for (manifestPath in changedManifestPaths) {
            val baselineManifestResult = runGit(
                projectRoot,
                "show",
                "$baseRef:$manifestPath"
            )
            if (baselineManifestResult.exitCode == 0) {
                try {
                    baselinePermissions += permissionScanner.parsePermissions(baselineManifestResult.output)
                } catch (parsingFailure: Exception) {
                    return PermissionBaselineResult(
                        permissions = emptyList(),
                        failureReason = "Unable to parse $manifestPath at $baseRef"
                    )
                }
            }
        }

        return PermissionBaselineResult(permissions = baselinePermissions.sorted())
    }

    private fun runGit(projectRoot: Path, vararg arguments: String): GitCommandResult {
        val process = ProcessBuilder(listOf("git", "-C", projectRoot.toString()) + arguments)
            .start()
        var errorOutput = byteArrayOf()
        val errorReader = Thread {
            errorOutput = process.errorStream.readAllBytes()
        }.apply(Thread::start)
        val output = process.inputStream.readAllBytes()
        val exitCode = process.waitFor()
        errorReader.join()

        return GitCommandResult(exitCode, output, errorOutput)
    }

    private fun Path.toGitRelativePath(projectRoot: Path): String =
        projectRoot.relativize(this).joinToString("/") { pathPart -> pathPart.toString() }

    private data class GitCommandResult(
        val exitCode: Int,
        val output: ByteArray,
        val errorOutput: ByteArray
    ) {
        val outputText: String
            get() = output.toString(Charsets.UTF_8)

        val errorText: String
            get() = errorOutput.toString(Charsets.UTF_8)
    }
}
