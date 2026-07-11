package io.github.sandroisu.releasely

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.testing.test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScanCommandTest {

    @Test
    fun successfulBaselineRunsPermissionComponentAndGradleRules() = withTestProject { projectRoot ->
        val result = command().test(scanArguments(projectRoot))

        assertEquals(0, result.statusCode)
        assertOutputOrder(
            result.stdout,
            "Releasely scan started",
            "Gradle Android configs: 1",
            "Manifest components: 1",
            "Permissions: 1",
            "Permission baseline: HEAD",
            "Finding scope:",
            "Findings: 3"
        )
        assertContains(result.stdout, "Rule: manifest.permission.dangerous.camera")
        assertContains(result.stdout, "Rule: manifest.component.exported.activity")
        assertContains(result.stdout, "Rule: gradle.release.minify_disabled")
        assertContains(result.stdout, "- HIGH: 1")
        assertContains(result.stdout, "- MEDIUM: 2")
    }

    @Test
    fun unavailableBaselineSkipsPermissionRulesAndKeepsComponentAndGradleFindings() =
        withTestProject { projectRoot ->
            val result = command().test(
                scanArguments(
                    projectRoot = projectRoot,
                    baseRef = MISSING_BASE_REF
                )
            )

            assertEquals(0, result.statusCode)
            assertContains(result.stdout, "Permission baseline unavailable:")
            assertContains(result.stdout, "Findings: 2")
            assertContains(result.stdout, "- MEDIUM: 2")
            assertContains(result.stdout, "Rule: manifest.component.exported.activity")
            assertContains(result.stdout, "Rule: gradle.release.minify_disabled")
            assertFalse(result.stdout.contains("manifest.permission.dangerous.camera"))
        }

    @Test
    fun unavailableBaselineWritesReportsWithOnlyComponentAndGradleFindings() =
        withTestProject { projectRoot ->
            val markdownPath = projectRoot.resolve("reports/baseline-failure.md")
            val jsonPath = projectRoot.resolve("reports/baseline-failure.json")
            val result = command().test(
                scanArguments(
                    projectRoot = projectRoot,
                    baseRef = MISSING_BASE_REF,
                    markdownPath = markdownPath,
                    jsonPath = jsonPath
                )
            )

            assertEquals(0, result.statusCode)
            assertContains(result.stdout, "Permission baseline unavailable:")
            assertTrue(Files.isRegularFile(markdownPath))
            assertTrue(Files.isRegularFile(jsonPath))

            val markdown = markdownPath.readText()
            val json = jsonPath.readText()
            assertContains(markdown, "# Releasely Report")
            assertContains(markdown, "Findings: 2")
            assertContains(markdown, "manifest.component.exported.activity")
            assertContains(markdown, "gradle.release.minify_disabled")
            assertFalse(markdown.contains("manifest.permission.dangerous.camera"))
            assertFalse(markdown.contains("Permission baseline unavailable"))

            assertContains(json, "\"findingsCount\": 2")
            assertContains(json, "\"findingsBySeverity\": {\"MEDIUM\": 2}")
            assertContains(json, "\"manifest.component.exported.activity\": 1")
            assertContains(json, "\"gradle.release.minify_disabled\": 1")
            assertFalse(json.contains("manifest.permission.dangerous.camera"))
            assertFalse(json.contains("Permission baseline unavailable"))
        }

    @Test
    fun unavailableBaselineWritesReportsBeforeFailingOnMedium() = withTestProject { projectRoot ->
        val markdownPath = projectRoot.resolve("reports/fail-medium.md")
        val jsonPath = projectRoot.resolve("reports/fail-medium.json")
        val result = command().test(
            scanArguments(
                projectRoot = projectRoot,
                baseRef = MISSING_BASE_REF,
                markdownPath = markdownPath,
                jsonPath = jsonPath,
                failOn = "MEDIUM"
            )
        )

        assertEquals(1, result.statusCode)
        assertTrue(Files.isRegularFile(markdownPath))
        assertTrue(Files.isRegularFile(jsonPath))
        assertContains(markdownPath.readText(), "Findings: 2")
        assertContains(jsonPath.readText(), "\"findingsCount\": 2")
        assertOutputOrder(
            result.stdout,
            "Markdown report written: $markdownPath",
            "JSON report written: $jsonPath",
            "Releasely failed because findings reached threshold: MEDIUM"
        )
    }

    @Test
    fun unavailableBaselineWritesReportsAndPassesWithOnlyMediumFindingsAtHighThreshold() =
        withTestProject { projectRoot ->
            val markdownPath = projectRoot.resolve("reports/pass-high.md")
            val jsonPath = projectRoot.resolve("reports/pass-high.json")
            val result = command().test(
                scanArguments(
                    projectRoot = projectRoot,
                    baseRef = MISSING_BASE_REF,
                    markdownPath = markdownPath,
                    jsonPath = jsonPath,
                    failOn = "HIGH"
                )
            )

            assertEquals(0, result.statusCode)
            assertTrue(Files.isRegularFile(markdownPath))
            assertTrue(Files.isRegularFile(jsonPath))
            assertContains(result.stdout, "Findings: 2")
            assertContains(result.stdout, "- MEDIUM: 2")
            assertFalse(result.stdout.contains("Releasely failed because findings reached threshold"))
        }

    private fun command() = ReleaselyCommand().subcommands(ScanCommand())

    private fun scanArguments(
        projectRoot: Path,
        baseRef: String = "HEAD",
        markdownPath: Path? = null,
        jsonPath: Path? = null,
        failOn: String? = null
    ): List<String> = buildList {
        add("scan")
        add("--path")
        add(projectRoot.toString())
        add("--base-ref")
        add(baseRef)
        markdownPath?.let { path ->
            add("--markdown-report")
            add(path.toString())
        }
        jsonPath?.let { path ->
            add("--json-report")
            add(path.toString())
        }
        failOn?.let { severity ->
            add("--fail-on")
            add(severity)
        }
    }

    private fun withTestProject(assertion: (Path) -> Unit) {
        val projectRoot = Files.createTempDirectory("releasely-command-test")
        try {
            val manifestFile = projectRoot.resolve("app/src/main/AndroidManifest.xml")
            manifestFile.parent.createDirectories()
            projectRoot.resolve("settings.gradle.kts").writeText(
                """
                rootProject.name = "releasely-command-test"
                include(":app")
                """.trimIndent()
            )
            projectRoot.resolve("app/build.gradle.kts").writeText(
                """
                plugins {
                    id("com.android.application")
                }
                android {
                    namespace = "com.example.releaselytest"
                    buildTypes {
                        release {
                            isMinifyEnabled = false
                        }
                    }
                }
                """.trimIndent()
            )
            manifestFile.writeText(manifest(includeCameraPermission = false))
            initializeRepository(projectRoot)
            manifestFile.writeText(manifest(includeCameraPermission = true))

            assertion(projectRoot)
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

    private fun runGit(projectRoot: Path, vararg arguments: String) {
        val process = ProcessBuilder(listOf("git", "-C", projectRoot.toString()) + arguments)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        check(exitCode == 0) { output }
    }

    private fun manifest(includeCameraPermission: Boolean): String = buildString {
        appendLine("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">")
        if (includeCameraPermission) {
            appendLine("    <uses-permission android:name=\"android.permission.CAMERA\" />")
        }
        appendLine("    <application>")
        appendLine("        <activity android:name=\".MainActivity\" android:exported=\"true\" />")
        appendLine("    </application>")
        appendLine("</manifest>")
    }

    private fun assertOutputOrder(output: String, vararg expectedFragments: String) {
        var previousIndex = -1
        expectedFragments.forEach { fragment ->
            val fragmentIndex = output.indexOf(fragment)
            assertTrue(fragmentIndex >= 0, "Missing output fragment: $fragment")
            assertTrue(fragmentIndex > previousIndex, "Output fragment is out of order: $fragment")
            previousIndex = fragmentIndex
        }
    }

    private companion object {
        const val MISSING_BASE_REF = "definitely-missing-ref-for-test"
    }
}
