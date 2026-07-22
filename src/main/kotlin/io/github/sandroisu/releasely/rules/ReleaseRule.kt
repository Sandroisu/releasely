package io.github.sandroisu.releasely.rules

import io.github.sandroisu.releasely.GradleAndroidConfig
import io.github.sandroisu.releasely.scanners.manifrest.ManifestComponent
import io.github.sandroisu.releasely.ReleaseFinding
import java.nio.file.Path

data class ReleaseRuleContext(
    val projectPath: Path,
    val permissions: List<String>,
    val manifestComponents: List<ManifestComponent>,
    val gradleAndroidConfigs: List<GradleAndroidConfig> = emptyList()
)

fun interface ReleaseRule {
    fun evaluate(context: ReleaseRuleContext): List<ReleaseFinding>
}
