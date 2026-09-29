---
phase: 4
title: "Reusable ownership enforcement"
status: completed
priority: P1
effort: "6h"
dependencies: [3]
---

# Phase 4: Reusable ownership enforcement

## Objective

Ship the one helper every later slice uses to enforce server-side ownership, and the reusable test
support that proves a slice honours it. This is the issue's "reusable ownership enforcement tests"
deliverable.

## Context

`docs/api-contract-conventions.md` already fixed the shape: **a resource owned by another user returns
`not-found`, not `forbidden`**. `ARCHITECTURE_TECHNOLOGY_DECISIONS.md:140` requires every user-owned
query and mutation to enforce ownership on the server and never trust a client-supplied `userId`.

MVP 0 has no domain resource yet, so the helper is proven against a test-only owned resource. That
fixture must live outside `com.rikkaus.wealth` for the reason
`com.rikkaus.testfixtures.ProblemFixtureController` documents: a controller inside the scan root would
be serialized into the published contract.

## Requirements

### `UserOwned` and `OwnershipGuard`

```java
public interface UserOwned {
    UUID ownerId();
}

public final class OwnershipGuard {
    public static <T extends UserOwned> T requireOwned(Optional<T> candidate, UUID viewerId) { ... }
}
```

`requireOwned` throws `new ApiException(ProblemType.NOT_FOUND, ProblemType.NOT_FOUND.safeDetail())`
for **both** an absent record and a record owned by someone else, deliberately producing the identical
response. The detail text comes from the taxonomy rather than the call site so no caller can
accidentally write a message that distinguishes the two cases — that is the whole non-disclosure
property, and leaving the text to callers is how it would eventually be lost.

### `CurrentUser`

The viewer id comes from the authenticated principal only. A companion ArchUnit rule forbids a
`@RestController` method parameter named `userId`, so the "never trust a client-supplied `userId`"
requirement is enforced mechanically for every future slice rather than by review habit.

### Reusable test support

- `AuthenticatedClient` from Phase 3, extended with `asSecondUser()` so a cross-user test needs no
  setup of its own.
- `OwnershipContract`, an abstract IT base a later slice extends by supplying a path, a record it
  owns and a record another user owns. It then inherits assertions for: owner reads 200, non-owner
  reads 404, non-owner writes 404, anonymous gets 401, and the non-owner 404 body is byte-identical to
  a genuinely missing record's body.

The byte-identical assertion is the one that actually protects the property. Comparing status codes
alone would pass while `detail` or an extension member leaked the difference.

## Files

| Action | Path |
|---|---|
| Create | `.../shared/security/UserOwned.java` |
| Create | `.../shared/security/OwnershipGuard.java` |
| Modify | `.../shared/security/CurrentUser.java` |
| Create | `.../shared/security/OwnershipGuardTest.java` |
| Modify | `.../wealth/ArchitectureRulesTest.java` |
| Create | `src/test/java/com/rikkaus/testfixtures/OwnedRecordFixtureController.java` |
| Create | `.../support/OwnershipContract.java` |
| Modify | `.../support/AuthenticatedClient.java` |
| Create | `.../shared/security/OwnershipEnforcementIT.java` |

## Steps

1. **3.T** — Phase 3's suite green.
2. Write `OwnershipGuardTest` covering absent, owned, and other-user, asserting the thrown
   `ApiException` carries `NOT_FOUND` and the taxonomy's own detail in all failing cases.
3. Implement `UserOwned`, `OwnershipGuard` and the `CurrentUser` accessor.
4. Add the ArchUnit rule banning a client-supplied `userId` parameter, with `allowEmptyShould(true)`
   like its siblings, since it matches nothing until a domain slice arrives.
5. Build `OwnedRecordFixtureController` with an in-memory record owned by a known user, and
   `OwnershipContract`. Have `OwnershipEnforcementIT` extend it.
6. **3.V** — `mvnw verify` green.

## Validation

- `OwnershipGuardTest` green.
- `OwnershipEnforcementIT` green, including the byte-identical body comparison.
- The new ArchUnit rule is in the `ArchitectureRulesTest` run and does not fail on current code.
- `OpenApiContractIT.publishesOnlyTheProductionApiSurface` still passes, proving the new fixture did
  not leak into the contract.

## Risk

| Risk | Mitigation |
|---|---|
| A later slice forgets the guard entirely. | `OwnershipContract` makes the test cheap to inherit; the ArchUnit rule catches the specific "trusted client userId" mistake. Neither can force a slice to call the guard, so this is named as a residual gap in the report rather than claimed as solved. |
| The fixture leaks into the published contract. | Package outside the scan root, route off `/api/v1`, and the existing contract assertion. |

## Rollback

Delete the four new files and revert the two edits. Nothing in Phases 1–3 depends on this phase.
