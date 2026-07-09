package io.github.sandroisu.releasely

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path

class ReportFileWriter {

    fun write(path: Path, reportContent: String) {
        path.parent?.let(Files::createDirectories)
        Files.writeString(path, reportContent, UTF_8)
    }
}
