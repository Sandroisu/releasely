package io.github.sandroisu.releasely

class DebuggableReleaseRule : ReleaseRule {

    override fun evaluate(context: ReleaseRuleContext): List<ReleaseFinding> =
        context.gradleAndroidConfigs
            .filter { config ->
                config.androidPluginType == AndroidPluginType.APPLICATION &&
                    config.releaseDebuggable == true
            }
            .map { config ->
                ReleaseFinding(
                    ruleId = "gradle.release.debuggable_enabled",
                    severity = ReleaseFindingSeverity.HIGH,
                    title = "Release build is debuggable",
                    description = "Android application module explicitly enables debugging for the release build type. The resulting release artifact can expose debugging capabilities in production.",
                    evidence = listOf(
                        "Gradle file: ${config.gradleFile}",
                        "Android plugin type: ${config.androidPluginType}",
                        "releaseDebuggable=true"
                    ),
                    recommendation = "Set isDebuggable = false in the Kotlin DSL, or debuggable false in the Groovy DSL, inside the release build type before producing the release APK/AAB.",
                    locationPath = config.gradleFile.toString()
                )
            }
}
