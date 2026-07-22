package io.github.sandroisu.releasely.scanners.manifrest

import java.nio.file.Path

enum class ManifestComponentType {
    ACTIVITY,
    ACTIVITY_ALIAS,
    SERVICE,
    RECEIVER,
    PROVIDER
}

data class ManifestComponent(
    val manifestFile: Path,
    val type: ManifestComponentType,
    val name: String?,
    val exported: Boolean?,
    val hasIntentFilter: Boolean
)
