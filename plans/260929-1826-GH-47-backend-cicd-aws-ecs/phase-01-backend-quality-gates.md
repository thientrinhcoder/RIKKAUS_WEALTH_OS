---
phase: 1
title: "Backend quality gates in the Maven build"
status: done
issue: 47
dependencies: []
---

# Phase 1: Backend quality gates in the Maven build

## Goal

Make `./mvnw verify` the whole backend gate, so the pipeline runs one command rather than encoding
the definition of "checked" in workflow YAML where a second runner could not reproduce it.

## What changed

`services/api/pom.xml` gained two plugins, both pinned:

| Plugin | Version | Bound to |
|---|---|---|
| `spotless-maven-plugin` | 3.10.3 | `validate` |
| `jacoco-maven-plugin` | 0.8.15 | `test` and `post-integration-test` |

Failsafe and Surefire were already configured by issue #39 and were not touched.

## The formatter decision

The architecture names Spotless but not a formatter, so one had to be chosen. Both candidates were
run against the real source tree before deciding rather than picked by reputation:

| Candidate | Effect on this tree |
|---|---|
| `palantir-java-format` 2.100.0 | 437 changed lines across 17 files, mostly splitting `@Autowired private Foo foo;` |
| `google-java-format` 1.36.1, AOSP | 12 files rewrapped; with `formatJavadoc=false` still 12, because it rewraps `//` comments at 100 columns and this tree's longest line is 110 |

The second is the more revealing measurement. The sources here are wrapped by hand at sentence
boundaries and their comments carry the reasoning behind decisions; the AOSP pass strands `{@code`
at end of line and orphans single words like `proof.` onto their own comment line. Either formatter
would have traded readable explanation for mechanical column fitting, inside a pull request whose
subject is the deployment pipeline.

So no whole-file formatter is configured. Spotless enforces what rots silently and what nobody
argues about: unused imports, import order, trailing whitespace, final newline. Import order is
declared explicitly as `\#,` — static first, then one ASCII-sorted block — because that is what the
tree already uses and an unstated order is a rule that only announces itself by failing someone's
build.

This leaves layout unenforced. Adopting a formatter stays open as a separate change with a measured
cost and its own review; the reasoning is recorded in the POM so the next person does not have to
re-derive it.

## Coverage without a floor

JaCoCo reports the two tiers separately — `jacoco-unit.exec` from Surefire, `jacoco-integration.exec`
from Failsafe — and merges neither. A merged report lets integration coverage paper over an untested
unit, and reading them apart is how a thin unit tier stays visible.

No `check` rule is configured. A threshold invented here would be a number nobody agreed to, and the
first team to hit it deletes an assertion rather than writes one. Both reports upload as build
artifacts so a reviewer can read them; agreeing a floor is a separate, explicit decision.

## Verification

Run in a `eclipse-temurin:25.0.4.1_1-jdk-noble` container, because this workstation has only JDK 21
and the project targets 25:

```
spotless:check   25 files clean, 0 needing changes
Surefire         29 tests, 0 failures
Failsafe         17 tests, 0 failures   (Testcontainers PostgreSQL)
JaCoCo           jacoco-unit and jacoco-integration reports written
BUILD SUCCESS
```

The first run of the formatter gate on the existing tree produced zero violations, which was the
point of measuring before choosing.
