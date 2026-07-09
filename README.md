# Releasely

**Catch Android release risks before your users do.**

Releasely is a Kotlin/JVM CLI for Android release risk audit. It looks at an
Android project from a release point of view. It does not ask whether the code
is beautiful. It asks a more practical question: **is this build safe to ship?**

The project is at an early stage. Today, Releasely can:

- recognize Gradle and Android projects;
- find manifests without wandering into build caches;
- collect permissions across project manifests;
- flag newly added sensitive permissions against a Git baseline;
- report files it could not parse instead of quietly ignoring them;
- write Markdown and JSON findings reports;
- fail CI when findings reach a chosen severity threshold.

No backend, no account, no source upload. The scan runs where the code lives.

## Usage

You need JDK 21.

Basic scan:

```powershell
.\gradlew.bat run --args="scan --path C:\Users\alex\android"
```

Markdown report:

```powershell
.\gradlew.bat run --args="scan --path C:\Users\alex\android --markdown-report build/releasely/report.md"
```

JSON report:

```powershell
.\gradlew.bat run --args="scan --path C:\Users\alex\android --json-report build/releasely/report.json"
```

CI threshold:

```powershell
.\gradlew.bat run --args="scan --path C:\Users\alex\android --fail-on MEDIUM"
```

Combined run:

```powershell
.\gradlew.bat run --args="scan --path C:\Users\alex\android --markdown-report build/releasely/report.md --json-report build/releasely/report.json --fail-on MEDIUM"
```

Severity threshold order:

```text
INFO < LOW < MEDIUM < HIGH
```

When report options and `--fail-on` are used together, report files are written
before the command exits with failure.

By default, permission changes are compared with `HEAD`. To inspect a branch,
choose another Git ref:

```powershell
.\gradlew.bat run --args="scan --path C:\Users\alex\android --base-ref origin/main"
```

On Unix-like shells:

```bash
./gradlew run --args="scan --path ../my-android-app"
```

The current stdout is intentionally plain:

```text
.\gradlew.bat run --args="scan --path ..\my-android-app"
Releasely scan started
Path: ..\my-android-app
Gradle project: yes
Android project: yes
Findings: 2
```

## Where it is going

The useful version of Releasely should catch changes that are easy to miss in a
busy pull request: a new dangerous permission, an exported component, a broken
deep link, a release build without minification, or a version code that stayed
behind.

The path there is straightforward:

- richer manifest and Gradle inspection;
- broader git diff analysis beyond manifest permissions;
- focused release-risk rules with low noise;
- a GitHub Action that can leave the boring warnings before release day.

Deterministic checks come first. AI can explain and prioritize findings later,
but it should never invent the evidence.

## Current status

This is the first working slice, not a finished auditor. Expect the output and
CLI shape to evolve while the rule set grows.

Found a release mistake worth teaching Releasely about? Open an issue with the
smallest reproducible example you can share.
