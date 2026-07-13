# Releasely

**Catch Android release risks before your users do.**

Releasely is a deterministic static analyzer for Android release risks. It
inspects an Android project for changes and configurations that are easy to
miss before shipping, then produces structured findings for developers and CI.

The current version does not use AI. Findings come from deterministic scanners
and rules, so the same project state and scan inputs produce the same audit
result. The CLI and GitHub Action run without a Releasely account, backend, or
AI provider, and the repository stays on the machine or GitHub runner
performing the scan.

Today, Releasely can:

- recognize Gradle and Android projects;
- find manifests without wandering into build caches;
- collect permissions across project manifests;
- flag newly added sensitive permissions against a Git baseline;
- report files it could not parse instead of quietly ignoring them;
- write Markdown and JSON findings reports;
- fail CI when findings reach a chosen severity threshold.

## Quick Start

### GitHub Action (recommended)

Copy the ready-to-use
[Releasely workflow](examples/github-actions/releasely.yml) to
`.github/workflows/releasely.yml` in an Android repository. Until the first tag
is created, the example uses `sandroisu/releasely@main`.

The workflow runs automatically for every pull request. To run it manually,
open the repository's **Actions** tab, select **Releasely**, choose
**Run workflow**, select the minimum failing severity, and start the run.

Open the completed run to read the audit in **Job summary**. The same run's
**Artifacts** section contains the Markdown and JSON reports.

### Local CLI

With `releasely` available on `PATH`, scan the current Android project:

```shell
releasely scan --path .
```

To run Releasely from this repository's sources on the tracked smoke fixture,
use JDK 21:

```powershell
.\gradlew.bat run --args="scan --path fixtures/action-smoke"
```

On Unix-like shells:

```shell
./gradlew run --args="scan --path fixtures/action-smoke"
```

## CLI usage

Write a Markdown report:

```shell
releasely scan --path . --markdown-report build/releasely/report.md
```

Write a JSON report:

```shell
releasely scan --path . --json-report build/releasely/report.json
```

Fail CI when findings reach a threshold:

```shell
releasely scan --path . --fail-on MEDIUM
```

Write both reports and apply a threshold:

```shell
releasely scan --path . \
  --markdown-report build/releasely/report.md \
  --json-report build/releasely/report.json \
  --fail-on MEDIUM
```

Severity threshold order:

```text
INFO < LOW < MEDIUM < HIGH
```

When report options and `--fail-on` are used together, report files are written
before the command exits with failure.

By default, permission changes are compared with `HEAD`. To inspect a branch,
choose another Git ref:

```shell
releasely scan --path . --base-ref origin/main
```

## GitHub Action

The [copy-ready workflow](examples/github-actions/releasely.yml) supports both
pull request audits and manual runs with a selectable severity threshold.
`fetch-depth: 0` makes Git history available for permission comparison.
`base-ref` selects the pull request base revision; when it is empty, the Action
does not pass `--base-ref` and the CLI keeps its default behavior.

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

GitHub-hosted `ubuntu-latest` is supported. Self-hosted runners have not been
fully verified; the Node.js 24-based Action dependencies require GitHub Actions
Runner `2.327.1` or newer.

The Markdown report is appended to the run's **Job summary**, and both report
files are available under **Artifacts** on the workflow run page. Report and
summary steps run even when `fail-on` rejects the findings, so the evidence is
still available on a failed audit.

### First release plan

The first versioned release will be `v0.1.0`. After it is published, workflows
should use `sandroisu/releasely@v0`; the `v0` alias will follow the latest
compatible `0.x` release. The public Action contract is not stable enough for a
`v1` release yet.

## AI roadmap

Releasely does not currently use AI. Deterministic scanners and rules remain the
source of truth, and AI will never determine whether a rule matched.

The planned optional flow is:

```text
structured findings
-> optional AI explanation
-> release summary / remediation hints / QA checklist
```

The first planned AI integration follows a bring-your-own-key (BYOK) model:

- users provide their own API key through an environment variable or GitHub
  Secret, never through a CLI argument;
- Releasely does not store the key;
- the provider integration remains replaceable;
- by default, AI receives structured findings and evidence, not the entire
  repository;
- AI cannot change severity, rule results, or the CLI exit code;
- AI remains completely optional, and the CLI and Action continue to work
  without it.

No provider, configuration name, or public AI interface is defined yet.

## Roadmap

See [ROADMAP.md](ROADMAP.md) for the ordered v0.x product direction. The focus
remains deterministic Android release checks first, with optional explanation
layers later.

## Current status

This is the first working slice, not a finished auditor. Expect the output and
CLI shape to evolve while the rule set grows.

Found a release mistake worth teaching Releasely about? Open an issue with the
smallest reproducible example you can share.
