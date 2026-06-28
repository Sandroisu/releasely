package io.github.sandroisu.releasely

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class GitPermissionBaselineResolverTest {

    @Test
    fun `resolves permissions before local manifest changes`() {
        val projectRoot = Files.createTempDirectory("releasely-baseline-test")

        try {
            val manifestFile = projectRoot.resolve("app/src/main/AndroidManifest.xml")
            manifestFile.parent.createDirectories()
            manifestFile.writeText(manifestWith("android.permission.INTERNET"))
            initializeRepository(projectRoot)

            manifestFile.writeText(
                manifestWith(
                    "android.permission.INTERNET",
                    "android.permission.ACCESS_BACKGROUND_LOCATION"
                )
            )
            assertEquals(
                "app/src/main/AndroidManifest.xml",
                runGit(projectRoot, "diff", "--name-only", "HEAD").trim().replace('\\', '/')
            )

            val result = GitPermissionBaselineResolver().resolve(
                projectRoot = projectRoot,
                currentManifestFiles = listOf(manifestFile),
                baseRef = "HEAD"
            )

            assertEquals(null, result.failureReason)
            assertEquals(listOf("android.permission.INTERNET"), result.permissions)
        } finally {
            projectRoot.toFile().deleteRecursively()
        }
    }

    private fun initializeRepository(projectRoot: Path) {
        runGit(projectRoot, "init", "--quiet")
        runGit(projectRoot, "config", "core.autocrlf", "false")
        runGit(projectRoot, "config", "user.name", "Releasely Test")
        runGit(projectRoot, "config", "user.email", "releasely-test@example.invalid")
        runGit(projectRoot, "add", ".")
        runGit(projectRoot, "commit", "--quiet", "-m", "Baseline")
    }

    private fun runGit(projectRoot: Path, vararg arguments: String): String {
        val process = ProcessBuilder(listOf("git", "-C", projectRoot.toString()) + arguments)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        check(exitCode == 0) { output }
        return output
    }

    private fun manifestWith(vararg permissions: String): String = buildString {
        appendLine("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">")
        permissions.forEach { permission ->
            appendLine("    <uses-permission android:name=\"$permission\" />")
        }
        appendLine("</manifest>")
    }
}
