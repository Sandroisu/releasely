package io.github.sandroisu.releasely

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import java.nio.file.FileVisitOption
import java.nio.file.FileVisitResult
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.EnumSet

data class ProjectDetectionResult(
    val path: Path,
    val exists: Boolean,
    val isDirectory: Boolean,
    val isGradleProject: Boolean,
    val isAndroidProject: Boolean,
    val gradleFiles: List<Path>,
    val androidEvidence: List<Path>,
    val manifestFiles: List<Path>,
)

class ProjectDetector {

    private val ignoredDirs = setOf(
        "build",
        ".gradle",
        ".git",
        ".idea",
        "out",
        ".kotlin",
        "node_modules"
    )

    fun detect(path: Path): ProjectDetectionResult {
        val exists = Files.exists(path)
        val isDirectory = Files.isDirectory(path)

        if (!exists || !isDirectory) {
            return ProjectDetectionResult(
                path = path,
                exists = exists,
                isDirectory = isDirectory,
                isGradleProject = false,
                isAndroidProject = false,
                gradleFiles = emptyList(),
                androidEvidence = emptyList(),
                manifestFiles = emptyList(),
            )
        }
        val gradleFiles = findGradleFiles(path)
        val androidEvidenceFiles = gradleFiles.filter { file ->
            file.name == "build.gradle" ||
                file.name == "build.gradle.kts" ||
                file.name == "libs.versions.toml"
        }
        val isGradleProject =
            Files.exists(path.resolve("settings.gradle")) ||
                    Files.exists(path.resolve("settings.gradle.kts")) ||
                    gradleFiles.isNotEmpty()

        val androidEvidence = androidEvidenceFiles.filter { file ->
            val text = readTextOrEmpty(file)
            text.contains("com.android.") ||
                    text.contains("alias(libs.plugins.android") ||
                    text.contains("id(\"com.android") ||
                    text.contains("id 'com.android")
        }

        val manifestFiles = findFilesByName(path, "AndroidManifest.xml")
        return ProjectDetectionResult(
            path = path,
            exists = true,
            isDirectory = true,
            isGradleProject = isGradleProject,
            isAndroidProject = androidEvidence.isNotEmpty(),
            gradleFiles = gradleFiles,
            androidEvidence = androidEvidence,
            manifestFiles = manifestFiles,
        )
    }

    private fun readTextOrEmpty(file: Path): String =
        try {
            Files.readString(file)
        } catch (_: Exception) {
            ""
        }

    private fun findGradleFiles(root: Path): List<Path> =
        findFiles(root, maxDepth = 8) { file ->
            file.name == "build.gradle" ||
                    file.name == "build.gradle.kts" ||
                    file.name == "settings.gradle" ||
                    file.name == "settings.gradle.kts" ||
                    file.name == "libs.versions.toml"
        }

    private fun findFilesByName(root: Path, fileName: String): List<Path> =
        findFiles(root, maxDepth = 8) { file ->
            file.name == fileName
        }

    private fun findFiles(
        root: Path,
        maxDepth: Int,
        predicate: (Path) -> Boolean
    ): List<Path> {
        val result = mutableListOf<Path>()

        Files.walkFileTree(
            root,
            EnumSet.noneOf(FileVisitOption::class.java),
            maxDepth,
            object : SimpleFileVisitor<Path>() {

                override fun preVisitDirectory(
                    dir: Path,
                    attrs: BasicFileAttributes
                ): FileVisitResult {
                    if (dir != root && dir.name in ignoredDirs) {
                        return FileVisitResult.SKIP_SUBTREE
                    }

                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes
                ): FileVisitResult {
                    if (Files.isRegularFile(file) && predicate(file)) {
                        result.add(file)
                    }

                    return FileVisitResult.CONTINUE
                }
            }
        )

        return result
    }
}
