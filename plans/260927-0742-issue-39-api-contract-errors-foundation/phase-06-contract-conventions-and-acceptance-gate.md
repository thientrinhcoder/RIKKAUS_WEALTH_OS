---
phase: 6
title: "Contract conventions and acceptance gate"
status: done
priority: P1
effort: "5h"
issue: 39
dependencies: [1, 2, 3, 4, 5]
---

# Phase 6: Contract conventions and acceptance gate

## Goal

Document the conventions sibling teams must build against, correct the README claims this work
invalidates, notify the Frontend sibling, and record acceptance evidence for every #39 criterion from
real command output.

## Overview

This phase produces no production code. It exists because #39's own acceptance criteria are partly
about coordination, not implementation: the contract change must be "cập nhật cùng examples và thông
báo cho sibling tasks trước khi merge/approve", and the deliverable must meet the parent feature's
criteria that the client classify success and problem responses consistently. Neither is satisfied by
code alone.

It also has a correction job. `README.md:156-158` tells a reader the backend contains no OpenAPI or
RFC 9457 conventions, and `README.md:160-163` separately says it contains no unit, integration,
Testcontainers or ArchUnit tests and that issue #35's automated-test criterion is intentionally
unsatisfied. Phases 1 through 5 make all of that false. Note the two ranges: the first draft of this
plan cited only `:154-158`, which **misses the paragraph carrying the test claim** — the very paragraph
this phase most needs to correct. Both ranges are in scope.

`README.md:137` also documents that "backend CORS permits the browser origin", which Phase 2 now
actually implements for the first time, and the README's backend environment section needs
`API_ALLOWED_ORIGINS`. Phase 2 owns that one edit so the variable is not undocumented for a whole phase.

The honest-reporting duty runs in three directions, and all three are recorded rather than smoothed over.
Phase 4 delivers a readable console log pattern carrying the correlation ID, not JSON-encoded structured
logs, so the structured-logging criterion is partial and the encoder belongs to #47. Phase 1 repaired a
Flyway autoconfiguration that had never run, which means this work **does** change existing behavior and
the plan's blast-radius statement was corrected accordingly. And #39's ownership criterion is delivered
as a documented contract shape with enforcement deferred to #42 by explicit Product Owner decision, not
silently dropped.

## Context links

- Issue: [#39](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/39)
- Parent feature: [#8](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/8)
- Frontend sibling to notify: [#38](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/38)
- Successor that inherits the taxonomy: [#42](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/42)
- CI owner: [#47](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/47)
- README claims to correct: `README.md:156-158` (OpenAPI/RFC 9457) and `README.md:160-163` (no tests, #35 criterion)
- Definition of "API Ready": `RIKKAUS_WEALTH_OS_MVP_DELIVERY_BACKLOG.md:30`
- Frontend consumer pattern: `apps/mobile/src/features/health/health-client.ts`

## Requirements

### Functional

- [x] `docs/api-contract-conventions.md` documents the path convention, the error taxonomy, the
      correlation header, the contract location and the regeneration command.
- [x] `README.md` no longer claims the backend lacks OpenAPI, RFC 9457 or automated tests, and documents
      how to run both test tiers and regenerate the contract.
- [x] Every #39 acceptance criterion is recorded in
      `plans/reports/pm-260927-issue-39-acceptance.md` with the command run and its real result.
- [ ] #38 is notified on the issue with the contract path, the error schema and the header name.
- [x] Criteria this task does not fully satisfy are stated as unsatisfied, not omitted.

### Non-functional

- [x] No documented command is written from memory; each is executed and its output recorded.
- [x] Documentation links to source and to the generated contract instead of restating their contents.
- [x] The conventions document is short enough that a sibling engineer reads it fully.

## Architecture

Documentation is split by audience, which is why it is two files and not one.

`docs/api-contract-conventions.md` is for a sibling engineer who has to build against this API. It
answers: what is the base path, what shape is an error, what header do I send and read, where is the
contract, how do I regenerate it. It is the durable contract surface referenced by
`RIKKAUS_WEALTH_OS_MVP_DELIVERY_BACKLOG.md:30`'s "API Ready" definition.

`README.md` is for an engineer setting up the repository. It gets the commands and the corrected scope
statement, and links to the conventions document rather than duplicating it.

`plans/reports/pm-260927-issue-39-acceptance.md` is a stateful record, not evergreen documentation. It
captures what was verified on which date with what output, and it does not become product authority
because the phase completed.

## Files to create and modify

- Create: `docs/api-contract-conventions.md`
- Create: `plans/reports/pm-260927-issue-39-acceptance.md`
- Modify: `README.md` — both scope-limit paragraphs (`:156-158` and `:160-163`) and the backend
  verification commands
- Modify: `.gitignore` — allow-list this plan directory and the acceptance report

**Why `.gitignore` changes.** `plans/**/*` is ignored with an allow-list covering only
`plans/templates/*` and two 2026-09-13 plan directories, and `git ls-files plans/reports` returns zero
tracked files. So the acceptance report — the sole evidence artifact for #39's criteria, which step 6
links from a public comment on #38 — would never reach the pull request, and a reviewer approving #39
would have nothing to check the "every claim traces to real output" criterion against. The repository
already has the pattern for this exception at `.gitignore:71-75`; follow it for this plan directory and
the report.

## Implementation steps

1. **Write `docs/api-contract-conventions.md`.** Cover exactly these sections, each grounded in the code
   that now exists rather than in this plan's intentions.

   - *Base path and versioning.* All resources live under `/api/v1`, defined once in
     `ApiPaths.V1`. New versions get a new static prefix. Spring Framework 7's native
     `spring.mvc.apiversion` feature is deliberately not used, and say why: springdoc issue #3163
     reports it returns HTTP 400 from `/v3/api-docs` on Boot 4.x.
   - *The contract.* Location `services/api/openapi/openapi.json`, generated from the running
     application, committed, and enforced by `OpenApiContractIT`. Regenerate with
     `./services/api/mvnw -f services/api/pom.xml verify -Dopenapi.update=true`. State plainly that the
     flag is a deliberate local action and must never appear in a CI command, because that would disable
     drift detection entirely.
   - *Errors.* Every error is `application/problem+json` per RFC 9457. Tabulate the taxonomy from
     `ProblemType` with code, URN, title and status, including `unauthorized` and `forbidden` marked as
     reserved for #42. Document the `errors` extension member's shape and the `correlationId` member.
     Show one real response body, copied from actual output rather than invented.
   - *Correlation.* Clients may send `X-Correlation-Id`; it is echoed when it matches
     `^[A-Za-z0-9-]{8,64}$` and replaced with a generated UUID otherwise. It is returned on every
     response, exposed through CORS, and present on every Problem Details body. Say that a rejected value
     does not fail the request.
   - *Testing conventions.* `*Test` runs in Surefire with no Docker; `*IT` extends
     `AbstractPostgresIntegrationTest` and runs in Failsafe during `verify` with a Testcontainers
     PostgreSQL pinned to the Compose image tag.
   - *Ownership contract, enforcement deferred.* This section exists because #39's first acceptance
     criterion names ownership. Document the contract *shape* so #42 implements against an agreed
     interface rather than inventing one: `urn:rikkaus:problem:unauthorized` for a missing or invalid
     principal, `urn:rikkaus:problem:forbidden` for an authenticated principal without rights, and the
     convention that a resource owned by another user returns `not-found` rather than `forbidden`, so the
     API does not leak the existence of other users' records. State plainly that **no enforcement exists
     yet** and that `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:140` ("Every user-owned query and mutation must
     enforce ownership on the server. Never trust a client-supplied `userId`") is satisfied by #42, not
     here.
   - *What is not here.* Authentication and ownership enforcement (#42), CI gates including JaCoCo and
     Spotless (#47), JSON log encoding (#47), pagination, rate limiting and idempotency. Also state that
     the published contract omits `/actuator/health` by design, and that it currently contains no
     request-accepting endpoint and therefore no validation example, because Phase 3 keeps its test
     fixture off `/api/v1`. Naming the gaps stops a sibling from assuming a guarantee that does not exist.
   - *Contract consumption is hand-written this iteration.* `apps/mobile` has no OpenAPI codegen
     dependency and no generate script, so this contract is a review and documentation artifact now, not
     a generation input. Say so rather than implying #38 can generate a client from it today.

2. **Correct `README.md`.** Both paragraphs. Replace the `:156-158` claim so it no longer says the
   backend lacks OpenAPI and RFC 9457 conventions, and replace the `:160-163` paragraph so it no longer
   says there are no unit, integration, Testcontainers or ArchUnit tests. The new text must say that
   `/api/v1`, the published OpenAPI contract, RFC 9457 errors, correlation IDs and a two-tier test harness
   exist, and that authentication, ownership enforcement, CI/CD and domain behavior remain out of scope
   for their own tasks.

   Do not silently delete the #35 note about its unsatisfied automated-test criterion. State that #39
   added the harness #35 deliberately skipped, so that criterion is now satisfiable.

   Record the Flyway repair. A reader of the current README would reasonably believe Flyway has been
   applying migrations since #35; it has not. State that #39 added the missing `spring-boot-flyway`
   module and that migrations now actually run, and cite the before-and-after evidence Phase 1 captured.

   Add the backend verification commands: `test` for the fast tier, `verify` for the full tier (noting it
   requires Docker), the contract regeneration command with a warning that `-Dopenapi.update=true` is a
   deliberate local action and must never appear in a CI command, and a link to the conventions document.

3. **Run the full acceptance pass and record it.** Execute each command and paste the real result. Do not
   summarize from memory and do not record a command that was not run.

   ```bash
   ./services/api/mvnw -f services/api/pom.xml clean test
   ./services/api/mvnw -f services/api/pom.xml clean verify
   git check-ignore -v services/api/openapi/openapi.json
   jq '(.paths | keys), (.components.schemas.ProblemDetail.properties | keys)' services/api/openapi/openapi.json
   docker compose --env-file .env.example up -d --wait postgres
   ./services/api/mvnw -f services/api/pom.xml spring-boot:run
   curl -s http://localhost:8080/api/v1/meta | jq .
   curl -i -s http://localhost:8080/api/v1/does-not-exist
   curl -i -s -X POST http://localhost:8080/api/v1/meta
   curl -s -D - -o /dev/null -H 'X-Correlation-Id: bad value with spaces' http://localhost:8080/api/v1/meta | grep -i x-correlation-id
   ```

4. **Write the acceptance report.** One row per #39 and #8 criterion, each with the evidence command and
   the observed result. Mark a criterion satisfied only when its own command passed.

   Three rows must be recorded as partial rather than green, with reasons:

   - *Structured logging.* The correlation ID appears on every log line, but JSON encoding for log
     shipping belongs to #47.
   - *Ownership.* The contract shape is published and documented; server-side enforcement is #42. Quote
     the Product Owner decision so this reads as a recorded choice rather than an omission.
   - *"API Ready" from `RIKKAUS_WEALTH_OS_MVP_DELIVERY_BACKLOG.md:30`.* That definition has four limbs:
     OpenAPI/schema, error contract, `quyền sở hữu dữ liệu`, and a working fixture or endpoint. #39
     satisfies three. State explicitly that the ownership limb is **not** met, because
     `RIKKAUS_WEALTH_OS_MVP_DELIVERY_BACKLOG.md:56` gates #38, #41 and #44 on "#39 API Ready" — and #41
     is the *frontend* identity and ownership task, which must not start building an ownership UX against
     a backend that has none.

   Also record what was deliberately not built, so a later reader does not mistake absence for oversight:
   no authentication, no ownership enforcement, no domain resources, no CI pipeline, no JaCoCo or
   Spotless, no fabricated data.

   Record the Flyway finding as its own entry. It is the one item in this task that fixed a defect in
   previously-shipped code, it changes runtime behavior, and #35 closed with its Flyway acceptance box
   unchecked. A future reader should be able to see why that happened without re-deriving it.

5. **Verify the frontend integration is real, not assumed.** Start the API with the `local` profile and
   the Expo web app, then confirm from a browser that:

   - `GET /api/v1/meta` succeeds cross-origin and its `X-Correlation-Id` is readable from JavaScript.
   - `GET /actuator/health` succeeds cross-origin, so the **already-shipped** health screen works in a
     browser. This is the check the first draft omitted: its browser step exercised only `/api/v1/meta`,
     so it would have passed while the one consumer that exists stayed broken.

   A passing `@WebMvcTest` cannot prove browser behavior. Record what was observed, including the actual
   Expo origin, since `apps/mobile/package.json:38` pins no port.

6. **Notify #38 on the issue.** Post a comment giving the contract path, the regeneration command, the
   `ProblemDetail` schema shape including `correlationId` and `errors`, the `X-Correlation-Id` contract
   including the validation rule, the CORS origin property name, and the reserved `unauthorized` and
   `forbidden` codes so #38's error classifier can handle them before #42 lands. Link the conventions
   document.

   State the three limitations honestly in the same comment rather than letting #38 discover them: the
   contract omits `/actuator/health`, it contains no request-accepting endpoint and therefore no
   validation example, and `apps/mobile` has no codegen so the contract is documentation rather than a
   generation input today. Ask #38 whether it wants a codegen step, and if so record that it belongs to
   #38's scope.

7. **Confirm the plan matches what shipped.** Re-read `plan.md` and every phase file, and reconcile any
   statement that implementation proved wrong. If a decision changed during implementation, the plan is
   updated to match reality rather than left describing an intention. Answer or re-raise the three open
   questions on the plan index.

## Todo

- [x] Write `docs/api-contract-conventions.md` with all eight sections.
- [x] Copy a real Problem Details body into the document rather than inventing one.
- [x] Document the ownership contract shape with enforcement attributed to #42.
- [x] Correct **both** README scope-limit paragraphs, `:156-158` and `:160-163`.
- [x] Record the Flyway repair in the README.
- [x] Add the two test-tier commands and the regeneration command, with the CI warning about
      `-Dopenapi.update=true`.
- [x] Allow-list this plan directory and the acceptance report in `.gitignore`.
- [x] Confirm with `git check-ignore` that the acceptance report is now tracked.
- [x] Run every acceptance command and capture real output.
- [x] Write `plans/reports/pm-260927-issue-39-acceptance.md` with one row per criterion.
- [x] Record structured logging, ownership, and "API Ready" each as partially satisfied, with reasons.
- [x] Record the Flyway finding as its own entry.
- [x] Verify cross-origin access and header readability in a browser for **both** `/api/v1/meta` and
      `/actuator/health`.
- [ ] Post the notification comment on #38 including the three stated limitations.
- [x] Reconcile `plan.md` and all phase files with what actually shipped.
- [x] Answer or re-raise the open questions.

## Verification

```bash
./services/api/mvnw -f services/api/pom.xml clean verify
grep -n "OpenAPI\|Testcontainers\|ArchUnit" README.md
git check-ignore -v plans/reports/pm-260927-issue-39-acceptance.md
git status --short
git diff --stat
```

Pass conditions:

- `clean verify` passes from a clean state, including all `*IT` classes.
- `README.md` contains no surviving claim that the backend lacks OpenAPI, RFC 9457, or automated tests.
- `git check-ignore` exits non-zero for the acceptance report, meaning it is now trackable, and
  `git status` shows it as a new file.
- `docs/api-contract-conventions.md` exists and its taxonomy table matches `ProblemType` entry for entry.
- The acceptance report has an evidence command and observed result for every criterion, with no row
  marked satisfied without one, and three rows explicitly marked partial.
- Both browser checks succeeded, recorded with the actual Expo origin observed.
- The #38 comment is posted, states the three limitations, and is linked from the acceptance report.

## Success criteria

- [x] A sibling engineer can build against this API from the conventions document alone.
- [x] The README no longer misleads a new engineer about what exists.
- [x] Every acceptance claim traces to a command and its real output.
- [x] Criteria that are not fully met are stated as such.
- [x] #38 has what it needs to build the typed client and its fixtures.

## Risk assessment

| Risk | Signal it broke | Response |
|---|---|---|
| Documentation restates the taxonomy and drifts from `ProblemType` as entries are added. | A code entry is missing from the document's table. | Phase 5 already enumerates the codes in the published contract schema, so the contract is the machine-checked source. The document's table links to `ProblemType` and to the generated contract, and Phase 5's drift assertion catches an undocumented code. |
| The acceptance report records intentions rather than executed results, which is the failure mode this phase exists to prevent. | A row has a command but no captured output. | No row may be marked satisfied without pasted output. A criterion that could not be verified is recorded as unverified with the reason, exactly as issue #35's plan did for its Docker-dependent checks. |
| The browser CORS check is skipped because the unit tests pass, and the frontend discovers the failure later. | Step 5 has no recorded observation. | It is a required step with its own checkbox. A passing `@WebMvcTest` cannot prove browser behavior, and this is the only end-to-end proof the parent feature's client criterion is reachable. |
| The structured-logging criterion is marked green because a correlation ID appears in logs. | The acceptance report claims full satisfaction. | Step 4 requires it be recorded as partial, with the JSON encoder attributed to #47. Overstating it would mislead the Feature Integrator who accepts #8. |
| #38 starts building against a contract that then changes, wasting their work. | A contract diff lands after the notification. | The notification includes the drift-detection mechanism and the obligation to re-notify. #39's own coordination criterion makes re-notification mandatory before merge, not optional. |

## Security considerations

Two disclosure checks before anything is written. The conventions document and the acceptance report
must contain no credential, token, connection string or real personal data; the Compose credentials in
`.env.example` are local-only placeholders and must not be copied into documentation as though they
were configuration guidance. Captured command output must be read before pasting, since `verify` output
can include environment details and a datasource URL.

The document states that authentication is absent and owned by #42. That is a deliberate disclosure to
internal siblings, and it is also why this API must not be exposed beyond local development until #42
lands. Say so in the document rather than leaving it implied.

## Next steps

With #39 closed, [#42](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/42) extends the
reserved `unauthorized` and `forbidden` taxonomy entries and adds ownership enforcement, and
[#47](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/47) wires `verify`, coverage,
formatting and JSON log encoding into CI.

## Implementation notes — 2026-09-27

All documentation, the `.gitignore` allow-list, the full acceptance pass and the browser verification are
done. **The one outstanding item is the notification comment on #38**, which is a public post on someone
else's issue and needs the Product Owner's go-ahead rather than being posted unilaterally. Both its
checkboxes are deliberately left unticked, and the matching acceptance criterion on the plan index is
left unticked too, so the gap is visible rather than implied. The comment is drafted and ready to post.

### Both README paragraphs were corrected, and the second one mattered most

`grep` now returns nothing for "no unit, integration, Testcontainers" or "full OpenAPI/RFC 9457
conventions". The #35 note was rewritten rather than deleted: it now says the automated-test criterion
was intentionally unsatisfied because no harness existed, and that this foundation added the harness, so
the criterion is satisfiable. The Flyway repair is recorded in its own README subsection with a link to
the acceptance report, because a reader of the previous README would reasonably have believed migrations
had been running since #35.

`API_ALLOWED_ORIGINS` went into the README's local-configuration section as a table alongside the
existing Postgres variables, during Phase 2 as the ownership exception allowed, so it was never
undocumented.

### The acceptance report is genuinely reachable by a reviewer

`plans/**/*` is ignored with a short allow-list, so without this the sole evidence artifact for the
issue's criteria would never have reached the pull request. Following the existing exception pattern,
the allow-list now covers this plan directory and the report. `git add --dry-run` confirms both are
trackable and `git status` lists them as new.

### Every acceptance command was executed

`clean test` and `clean verify` both exited 0, at 29 Surefire and 17 Failsafe tests. The live checks ran
against a running application on the `local` profile and their real output is pasted into the report
rather than summarised. Nothing is recorded that was not run.

### Three criteria are recorded as less than green

Structured logging is partial: the identifier reaches every log line but the pattern is readable text,
not JSON, and the encoder belongs to the CI/CD task. Ownership is a published contract shape with no
enforcement, by explicit Product Owner decision. "API Ready" satisfies three of its four limbs, and the
report states limb by limb that `quyền sở hữu dữ liệu` is **not met** — which matters because
`RIKKAUS_WEALTH_OS_MVP_DELIVERY_BACKLOG.md:56` gates #41, the frontend identity and ownership task, on
"#39 API Ready". #38 is genuinely unblocked; #41 is not.

### Plan reconciliation

`plan.md` now carries a table of the seven corrections implementation forced on it, and answers to all
three open questions. The Expo origin was answered empirically, the build-timestamp question needed no
escalation because the response never exposes one, and the codegen question is recorded as belonging to
#38's scope with the facts it needs to decide.
