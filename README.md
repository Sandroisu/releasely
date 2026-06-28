# Releasely

**Catch Android release risks before your users do.**

Releasely is a small Kotlin CLI that looks at an Android project from a release
point of view. It does not ask whether the code is beautiful. It asks a more
practical question: **is this build safe to ship?**

The project is at an early stage. Today, Releasely can:

- recognize Gradle and Android projects;
- find manifests without wandering into build caches;
- collect permissions across project manifests;
- flag newly added sensitive permissions against a Git baseline;
- report files it could not parse instead of quietly ignoring them.

No backend, no account, no source upload. The scan runs where the code lives.

## Try it

You need JDK 21. Point the CLI at an Android project:

```bash
./gradlew run --args="scan --path ../my-android-app"
```

By default, permission changes are compared with `HEAD`. To inspect a branch,
choose another Git ref:

```bash
./gradlew run --args="scan --path ../my-android-app --base-ref origin/main"
```

On Windows:

```powershell
.\gradlew.bat run --args="scan --path ..\my-android-app"
```

The current report is intentionally plain:

```text
Releasely scan started
Project exists: yes
Gradle project: yes
Android project: yes
Manifest files: 3
Permissions: 7
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
- Markdown and JSON reports;
- a GitHub Action that can leave the boring warnings before release day.

Deterministic checks come first. AI can explain and prioritize findings later,
but it should never invent the evidence.

## Current status

This is the first working slice, not a finished auditor. Expect the output and
CLI shape to evolve while the rule set grows.

Found a release mistake worth teaching Releasely about? Open an issue with the
smallest reproducible example you can share.
