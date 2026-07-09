package io.github.sandroisu.releasely

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path

class MarkdownReportFileWriter {

    fun write(path: Path, markdownReport: String) {
        path.parent?.let(Files::createDirectories)
        Files.writeString(path, markdownReport, UTF_8)
    }
}
