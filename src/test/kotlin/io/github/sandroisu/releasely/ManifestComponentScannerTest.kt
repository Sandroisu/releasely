package io.github.sandroisu.releasely

import io.github.sandroisu.releasely.scanners.manifrest.ManifestComponent
import io.github.sandroisu.releasely.scanners.manifrest.ManifestComponentScanner
import io.github.sandroisu.releasely.scanners.manifrest.ManifestComponentType
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ManifestComponentScannerTest {

    private val scanner = ManifestComponentScanner()

    @Test
    fun extractsActivityName() = withComponent("activity", ".MainActivity") { component ->
        assertEquals(ManifestComponentType.ACTIVITY, component.type)
        assertEquals(".MainActivity", component.name)
    }

    @Test
    fun extractsServiceName() = withComponent("service", ".SyncService") { component ->
        assertEquals(ManifestComponentType.SERVICE, component.type)
        assertEquals(".SyncService", component.name)
    }

    @Test
    fun extractsReceiverName() = withComponent("receiver", ".BootReceiver") { component ->
        assertEquals(ManifestComponentType.RECEIVER, component.type)
        assertEquals(".BootReceiver", component.name)
    }

    @Test
    fun extractsProviderName() = withComponent("provider", ".DataProvider") { component ->
        assertEquals(ManifestComponentType.PROVIDER, component.type)
        assertEquals(".DataProvider", component.name)
    }

    @Test
    fun extractsActivityAliasName() = withComponent("activity-alias", ".LauncherAlias") { component ->
        assertEquals(ManifestComponentType.ACTIVITY_ALIAS, component.type)
        assertEquals(".LauncherAlias", component.name)
    }

    @Test
    fun extractsExportedTrue() = withComponent("activity", ".MainActivity", "android:exported=\"true\"") { component ->
        assertTrue(component.exported == true)
    }

    @Test
    fun extractsExportedFalse() = withComponent("service", ".SyncService", "android:exported=\"false\"") { component ->
        assertEquals(false, component.exported)
    }

    @Test
    fun returnsNullExportedWhenAttributeIsMissing() = withComponent("receiver", ".BootReceiver") { component ->
        assertNull(component.exported)
    }

    @Test
    fun detectsIntentFilter() = withManifest(
        """
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
            </intent-filter>
        </activity>
        """.trimIndent()
    ) { component ->
        assertTrue(component.hasIntentFilter)
    }

    private fun withComponent(
        tag: String,
        name: String,
        additionalAttributes: String = "",
        assertion: (ManifestComponent) -> Unit
    ) {
        withManifest("<$tag android:name=\"$name\" $additionalAttributes />", assertion)
    }

    private fun withManifest(componentXml: String, assertion: (ManifestComponent) -> Unit) {
        val directory = Files.createTempDirectory("releasely-component-test")
        try {
            val manifestFile = directory.resolve("AndroidManifest.xml")
            manifestFile.writeText(
                """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <application>
                        $componentXml
                    </application>
                </manifest>
                """.trimIndent()
            )

            val result = scanner.scan(listOf(manifestFile))

            assertEquals(1, result.scannedManifestCount)
            assertTrue(result.failedManifestFiles.isEmpty())
            assertion(result.components.single())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
