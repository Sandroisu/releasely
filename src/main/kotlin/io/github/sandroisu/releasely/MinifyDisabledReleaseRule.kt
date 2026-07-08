package io.github.sandroisu.releasely

class MinifyDisabledReleaseRule : ReleaseRule {

    override fun evaluate(context: ReleaseRuleContext): List<ReleaseFinding> =
        context.gradleAndroidConfigs
            .filter { config ->
                config.androidPluginType == AndroidPluginType.APPLICATION &&
                    config.minifyEnabled == false
            }
            .map { config ->
                ReleaseFinding(
                    ruleId = "gradle.release.minify_disabled",
                    severity = ReleaseFindingSeverity.MEDIUM,
                    title = "Release minification appears disabled",
                    description = "Android application module declares minifyEnabled=false. Disabled minification can increase APK/AAB size and may leave release code less optimized.",
                    evidence = listOf(
                        "Gradle file: ${config.gradleFile}",
                        "Android plugin type: ${config.androidPluginType ?: "unknown"}",
                        "minifyEnabled=false"
                    ),
                    recommendation = "Verify that the application release build is intentionally published without R8/minification. If this is not intended, enable minification for the release build type."
                )
            }
}
