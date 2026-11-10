---
phase: 5
title: "Manual verification"
status: completed
priority: P1
effort: "1d"
dependencies: [4]
---

# Phase 5: Manual verification

## Goal

Replace the automated coverage this task is not writing with something a person can rerun and a
reviewer can check: one guide that reproduces every case, and one record of what this run actually
returned.

## Requirements

- The guide starts from a clean machine: a disposable PostgreSQL, the `local` profile, and a way to
  obtain two different users' access tokens without a real Google account.
- Every case is a complete `curl` invocation with the expected status and the material part of the
  expected body. No case says "verify it works".
- Cases cover, at minimum:
  - all eleven categories accepted on create;
  - every required field missing, one case each, each naming its own field in `errors`;
  - a negative current value, an unknown currency, an unknown liquidity, an unknown category;
  - a future valuation date and a purchase date later than the valuation date;
  - a value with more precision than the column holds;
  - the full optional-field set present, and the full optional-field set absent;
  - a `PUT` that clears the optional fields by omitting them;
  - a `PUT` attempting `currentValue`, which must be ignored rather than silently applied;
  - read, edit and delete of another user's asset, each `404`, with the body compared byte for byte
    against a genuinely missing identifier;
  - an anonymous request, an expired token and a wrong-signature token, each `401`;
  - a malformed UUID in the path, a wrong method, and a non-JSON body;
  - delete, then read the same identifier.
- The evidence report records the real status, the real body excerpt and a verdict per case, plus the
  version of the application and the commit it ran against.
- Anything that failed and was then fixed is recorded as having failed, with what changed.

## Files to Create

- `docs/manual-test-guide-assets.md`
- `plans/reports/manual-verification-261103-2231-GH-53-asset-catalog-common-crud.md`

## Steps

1. Start PostgreSQL from `docker-compose.yml` and run the application under the `local` profile.
2. Mint two users' sessions through the stub Google path the guide documents.
3. Work the case list in order, pasting each real response into the evidence report as it happens.
4. Rerun the whole list once more at the end, after the last code change, so the evidence describes
   the committed state and not an intermediate one.

## Validation

The evidence report has a verdict for every case in the guide, and the counts agree.

## Risk and Rollback

The honest risk is evidence drifting from the code it describes. Step 4 is the mitigation: the final
run happens after the final change, and the report names the commit.
