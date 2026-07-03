package io.github.sandroisu.releasely

import java.nio.file.Path

data class ReleaseRuleContext(
    val projectPath: Path,
    val permissions: List<String>
)

fun interface ReleaseRule {
    fun evaluate(context: ReleaseRuleContext): List<ReleaseFinding>
}
