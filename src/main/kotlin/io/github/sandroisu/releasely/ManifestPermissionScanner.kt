package io.github.sandroisu.releasely

import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

data class ManifestPermissionScanResult(
    val scannedManifestCount: Int,
    val failedManifestFiles: List<Path>,
    val permissions: List<String>
)

class ManifestPermissionScanner {

    private companion object {
        private const val ANDROID_NAMESPACE_URI = "http://schemas.android.com/apk/res/android"
    }

    fun scan(manifestFiles: List<Path>): ManifestPermissionScanResult {
        val uniquePermissions = sortedSetOf<String>()
        val failedManifestFiles = mutableListOf<Path>()

        manifestFiles
            .filter { manifestFile -> Files.isRegularFile(manifestFile) }
            .forEach { manifestFile ->
                try {
                    uniquePermissions += readPermissions(manifestFile)
                } catch (parsingFailure: Exception) {
                    failedManifestFiles.add(manifestFile)
                }
            }

        return ManifestPermissionScanResult(
            scannedManifestCount = manifestFiles.size,
            failedManifestFiles = failedManifestFiles,
            permissions = uniquePermissions.toList()
        )
    }

    private fun readPermissions(manifestFile: Path): List<String> {
        val documentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        }

        val document = documentBuilderFactory
            .newDocumentBuilder()
            .parse(manifestFile.toFile())

        val usesPermissionNodes = document.getElementsByTagName("uses-permission")
        val permissions = mutableListOf<String>()

        for (nodeIndex in 0 until usesPermissionNodes.length) {
            val permissionElement = usesPermissionNodes.item(nodeIndex) as? Element ?: continue

            val permissionName = permissionElement
                .getAttributeNS(ANDROID_NAMESPACE_URI, "name")
                .takeIf { attributeValue -> attributeValue.isNotBlank() }
                ?: permissionElement
                    .getAttribute("android:name")
                    .takeIf { attributeValue -> attributeValue.isNotBlank() }

            if (permissionName != null) {
                permissions += permissionName
            }
        }

        return permissions
    }
}