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

## GitHub Action

Until the first versioned release, use `sandroisu/releasely@main`. A pull request
workflow can run the audit like this:

```yaml
name: Releasely

on:
  pull_request:

jobs:
  release-audit:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - uses: sandroisu/releasely@main
        with:
          path: .
          base-ref: ${{ github.event.pull_request.base.sha }}
          fail-on: HIGH
```

`fetch-depth: 0` makes the Git history available for permission comparison.
`base-ref` selects the Git revision whose manifest permissions form the
baseline; when it is empty, the Action does not pass `--base-ref` and the CLI
keeps its default behavior.

Action inputs:

| Input | Default | Purpose |
| --- | --- | --- |
| `path` | `.` | Android project path, relative to the checked-out workspace or absolute. |
| `base-ref` | empty | Git reference used for the permission baseline. |
| `fail-on` | `HIGH` | Lowest severity that fails the step: `INFO`, `LOW`, `MEDIUM`, or `HIGH`. Empty disables threshold failure. |
| `markdown-report` | `build/releasely/report.md` | Markdown report path relative to the workspace. |
| `json-report` | `build/releasely/report.json` | JSON report path relative to the workspace. |
| `upload-artifacts` | `true` | Upload both reports with `actions/upload-artifact`. |
| `artifact-name` | `releasely-reports` | Name of the uploaded report artifact. |

Relative project and report paths are resolved from `GITHUB_WORKSPACE`.
Absolute report paths must still stay inside that workspace so GitHub can
upload them. The Action exposes the normalized absolute paths as
`markdown-report-path` and `json-report-path` outputs.

The Markdown report is appended to the run's **Job summary**, and both report
files are available under **Artifacts** on the workflow run page. Report and
summary steps run even when `fail-on` rejects the findings, so the evidence is
still available on a failed audit.

### First release plan

The first versioned release will be `v0.1.0`. After it is published, workflows
should use `sandroisu/releasely@v0`; the `v0` alias will follow the latest
compatible `0.x` release. The public Action contract is not stable enough for a
`v1` release yet.

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
- CI annotations and more focused pull request feedback.

Deterministic checks come first. AI can explain and prioritize findings later,
but it should never invent the evidence.

## Current status

This is the first working slice, not a finished auditor. Expect the output and
CLI shape to evolve while the rule set grows.

Found a release mistake worth teaching Releasely about? Open an issue with the
smallest reproducible example you can share.
