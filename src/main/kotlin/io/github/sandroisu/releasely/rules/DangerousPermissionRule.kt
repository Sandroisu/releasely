package io.github.sandroisu.releasely.rules

import io.github.sandroisu.releasely.ReleaseFinding
import io.github.sandroisu.releasely.ReleaseFindingSeverity

class DangerousPermissionRule : ReleaseRule {

    override fun evaluate(context: ReleaseRuleContext): List<ReleaseFinding> =
        context.permissions
            .distinct()
            .mapNotNull(::findingFor)

    private fun findingFor(permission: String): ReleaseFinding? {
        val (ruleId, severity) = when (permission) {
            "android.permission.ACCESS_FINE_LOCATION" ->
                "manifest.permission.dangerous.access_fine_location" to ReleaseFindingSeverity.HIGH
            "android.permission.ACCESS_COARSE_LOCATION" ->
                "manifest.permission.dangerous.access_coarse_location" to ReleaseFindingSeverity.HIGH
            "android.permission.CAMERA" ->
                "manifest.permission.dangerous.camera" to ReleaseFindingSeverity.HIGH
            "android.permission.POST_NOTIFICATIONS" ->
                "manifest.permission.dangerous.post_notifications" to ReleaseFindingSeverity.MEDIUM
            "android.permission.READ_EXTERNAL_STORAGE" ->
                "manifest.permission.dangerous.read_external_storage" to ReleaseFindingSeverity.MEDIUM
            "android.permission.WRITE_EXTERNAL_STORAGE" ->
                "manifest.permission.dangerous.write_external_storage" to ReleaseFindingSeverity.MEDIUM
            "android.permission.BLUETOOTH_SCAN" ->
                "manifest.permission.dangerous.bluetooth_scan" to ReleaseFindingSeverity.MEDIUM
            "android.permission.BLUETOOTH_CONNECT" ->
                "manifest.permission.dangerous.bluetooth_connect" to ReleaseFindingSeverity.MEDIUM
            else -> return null
        }

        return ReleaseFinding(
            ruleId = ruleId,
            severity = severity,
            title = "Dangerous permission detected",
            description = "Android manifest declares $permission. This permission can affect runtime permission flow, QA scope, and store privacy review.",
            evidence = listOf(permission),
            recommendation = "Verify that this permission is required, covered by runtime permission UX, tested in release QA, and reflected in privacy declarations if applicable.",
            locationPath = null
        )
    }
}
