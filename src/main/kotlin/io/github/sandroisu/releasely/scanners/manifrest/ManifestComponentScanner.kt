package io.github.sandroisu.releasely.scanners.manifrest

import io.github.sandroisu.releasely.secureDocumentBuilderFactory
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path

data class ManifestComponentScanResult(
    val scannedManifestCount: Int,
    val failedManifestFiles: List<Path>,
    val components: List<ManifestComponent>
)

class ManifestComponentScanner {

    private companion object {
        private const val ANDROID_NAMESPACE_URI = "http://schemas.android.com/apk/res/android"

        private val componentTypes = mapOf(
            "activity" to ManifestComponentType.ACTIVITY,
            "activity-alias" to ManifestComponentType.ACTIVITY_ALIAS,
            "service" to ManifestComponentType.SERVICE,
            "receiver" to ManifestComponentType.RECEIVER,
            "provider" to ManifestComponentType.PROVIDER
        )
    }

    fun scan(manifestFiles: List<Path>): ManifestComponentScanResult {
        val components = mutableListOf<ManifestComponent>()
        val failedManifestFiles = mutableListOf<Path>()

        manifestFiles
            .filter(Files::isRegularFile)
            .forEach { manifestFile ->
                try {
                    components += readComponents(manifestFile)
                } catch (parsingFailure: Exception) {
                    failedManifestFiles.add(manifestFile)
                }
            }

        return ManifestComponentScanResult(
            scannedManifestCount = manifestFiles.size,
            failedManifestFiles = failedManifestFiles,
            components = components
        )
    }

    private fun readComponents(manifestFile: Path): List<ManifestComponent> =
        Files.newInputStream(manifestFile).use { manifestInput ->
            val document = secureDocumentBuilderFactory()
                .newDocumentBuilder()
                .parse(manifestInput)
            val elements = document.getElementsByTagName("*")

            buildList {
                for (elementIndex in 0 until elements.length) {
                    val element = elements.item(elementIndex) as? Element ?: continue
                    val type = componentTypes[element.tagName] ?: continue
                    add(
                        ManifestComponent(
                            manifestFile = manifestFile,
                            type = type,
                            name = element.androidAttribute("name"),
                            exported = element.androidAttribute("exported")?.toBooleanStrictOrNull(),
                            hasIntentFilter = element.getElementsByTagName("intent-filter").length > 0
                        )
                    )
                }
            }
        }

    private fun Element.androidAttribute(name: String): String? =
        getAttributeNS(ANDROID_NAMESPACE_URI, name)
            .takeIf(String::isNotBlank)
}
