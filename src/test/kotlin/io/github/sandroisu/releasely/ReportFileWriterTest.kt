package io.github.sandroisu.releasely

import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReportFileWriterTest {

    private val writer = ReportFileWriter()

    @Test
    fun createsParentDirectories() {
        val directory = Files.createTempDirectory("releasely-markdown-report-test")
        try {
            val reportPath = directory.resolve("reports/releasely/report.md")

            writer.write(reportPath, "# Releasely Report")

            assertTrue(Files.exists(reportPath.parent))
            assertEquals("# Releasely Report", reportPath.readText())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun writesMarkdownContent() {
        val directory = Files.createTempDirectory("releasely-markdown-report-test")
        try {
            val reportPath = directory.resolve("report.md")

            writer.write(reportPath, "# Releasely Report")

            assertEquals("# Releasely Report", reportPath.readText())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun writesJsonContent() {
        val directory = Files.createTempDirectory("releasely-markdown-report-test")
        try {
            val reportPath = directory.resolve("report.json")

            writer.write(reportPath, "{\"findingsCount\":2}")

            assertEquals("{\"findingsCount\":2}", reportPath.readText())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun overwritesExistingFile() {
        val directory = Files.createTempDirectory("releasely-markdown-report-test")
        try {
            val reportPath = directory.resolve("report.md")
            reportPath.writeText("old")

            writer.write(reportPath, "new")

            assertEquals("new", reportPath.readText())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun throwsWhenParentPathIsNotDirectory() {
        val directory = Files.createTempDirectory("releasely-markdown-report-test")
        try {
            val parentFile = directory.resolve("blocked")
            parentFile.writeText("not a directory")
            val reportPath = parentFile.resolve("report.md")

            assertFailsWith<Exception> {
                writer.write(reportPath, "# Releasely Report")
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
