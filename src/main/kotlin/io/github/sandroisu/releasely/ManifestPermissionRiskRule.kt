package io.github.sandroisu.releasely

class ManifestPermissionRiskRule {

    fun evaluate(
        currentPermissions: List<String>,
        baselinePermissions: List<String>
    ): List<ReleaseFinding> {
        val baselinePermissionSet = baselinePermissions.toSet()

        return currentPermissions
            .distinct()
            .filterNot(baselinePermissionSet::contains)
            .mapNotNull(::findingFor)
    }

    private fun findingFor(permission: String): ReleaseFinding? =
        when (permission) {
            "android.permission.SYSTEM_ALERT_WINDOW" -> ReleaseFinding(
                ruleId = "manifest.permission.system_alert_window",
                severity = ReleaseFindingSeverity.HIGH,
                title = "Sensitive overlay permission added",
                description = "This change adds SYSTEM_ALERT_WINDOW, which requires strong justification and may affect review and security.",
                evidence = listOf(permission),
                recommendation = "Confirm that drawing over other apps is essential and review the permission's security and store-policy impact."
            )

            "android.permission.ACCESS_BACKGROUND_LOCATION" -> ReleaseFinding(
                ruleId = "manifest.permission.background_location",
                severity = ReleaseFindingSeverity.HIGH,
                title = "Background location permission added",
                description = "This change adds privacy-sensitive background location access.",
                evidence = listOf(permission),
                recommendation = "Verify the background location use case, user disclosure, runtime flow, and store-policy compliance."
            )

            "android.permission.READ_EXTERNAL_STORAGE" -> legacyStorageFinding(permission)
            "android.permission.WRITE_EXTERNAL_STORAGE" -> legacyStorageFinding(permission)
            else -> null
        }

    private fun legacyStorageFinding(permission: String): ReleaseFinding =
        ReleaseFinding(
            ruleId = when (permission) {
                "android.permission.READ_EXTERNAL_STORAGE" -> "manifest.permission.read_external_storage"
                else -> "manifest.permission.write_external_storage"
            },
            severity = ReleaseFindingSeverity.MEDIUM,
            title = "Legacy storage permission added",
            description = "This change adds a legacy storage permission whose behavior depends on targetSdk and scoped storage.",
            evidence = listOf(permission),
            recommendation = "Check whether the permission is still needed for the targetSdk and migrate to scoped storage APIs where possible."
        )
}
