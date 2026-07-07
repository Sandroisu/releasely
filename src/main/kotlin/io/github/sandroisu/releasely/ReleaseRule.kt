package io.github.sandroisu.releasely

import java.nio.file.Path

data class ReleaseRuleContext(
    val projectPath: Path,
    val permissions: List<String>,
    val manifestComponents: List<ManifestComponent>
)

fun interface ReleaseRule {
    fun evaluate(context: ReleaseRuleContext): List<ReleaseFinding>
}
