# API contract conventions

What you need to build against the Rikkaus Wealth OS backend. Everything here is implemented and
verified, not planned. Where something is deliberately absent, this document says so rather than
leaving you to discover it.

## Base path and versioning

Every resource lives under `/api/v1`, defined once as `ApiPaths.V1` in
[`ApiPaths.java`](../services/api/src/main/java/com/rikkaus/wealth/shared/api/ApiPaths.java). No other
file contains the literal. A new major version gets a new static prefix.

Spring Framework 7's native `spring.mvc.apiversion` header and path-segment versioning is deliberately
unused. springdoc [issue #3163](https://github.com/springdoc/springdoc-openapi/issues/3163) reports
that enabling it makes both `/v3/api-docs` and `/swagger-ui.html` return HTTP 400 on Boot 4.x. A static
path prefix is unaffected and must stay that way.

## The contract

| | |
|---|---|
| Location | [`services/api/openapi/openapi.json`](../services/api/openapi/openapi.json) |
| How it is produced | Generated from the running application by `OpenApiContractIT` during `verify` |
| How it is enforced | The same test fails the build when the generated and committed documents differ |
| Regenerate | `./services/api/mvnw -f services/api/pom.xml verify -Dopenapi.update=true` |

`-Dopenapi.update=true` is a deliberate local action. **It must never appear in a CI command**, because
it turns the drift check into a silent rewrite and the contract would then change without anyone
reading the diff. For the same reason `-DskipITs` is forbidden in CI: it disables the drift gate
entirely along with every other integration test.

Regenerating is not the end of the job. Any contract change must ship with updated examples and a note
on the affected sibling task before merge.

Live, the document is served at `/v3/api-docs` and browsable at `/swagger-ui/index.html`, **but only
under the `local` Spring profile**. Both return 404 otherwise. That gate is on the document endpoint,
not just the UI, because the contract describes every route and the whole error taxonomy on a service
that has no authentication yet.

## Errors

Every error under `/api/v1/**` is `application/problem+json` conforming to
[RFC 9457](https://www.rfc-editor.org/rfc/rfc9457), including framework-raised 400, 404, 405, 406 and
415. There is no status that returns Boot's default error shape.

The taxonomy is [`ProblemType`](../services/api/src/main/java/com/rikkaus/wealth/shared/error/ProblemType.java).
The `type` is a URN rather than a URL because this project owns no domain and an `https://` type would
name a host that resolves to nothing; RFC 9457 does not require the type to dereference.

| Code | `type` | Title | Status |
|---|---|---|---|
| `validation-failed` | `urn:rikkaus:problem:validation-failed` | Request validation failed | 400 |
| `malformed-request` | `urn:rikkaus:problem:malformed-request` | Malformed request | 400 |
| `not-found` | `urn:rikkaus:problem:not-found` | Resource not found | 404 |
| `method-not-allowed` | `urn:rikkaus:problem:method-not-allowed` | Method not allowed | 405 |
| `not-acceptable` | `urn:rikkaus:problem:not-acceptable` | Not acceptable | 406 |
| `unsupported-media-type` | `urn:rikkaus:problem:unsupported-media-type` | Unsupported media type | 415 |
| `unauthorized` | `urn:rikkaus:problem:unauthorized` | Authentication required | 401 |
| `forbidden` | `urn:rikkaus:problem:forbidden` | Access denied | 403 |
| `internal-error` | `urn:rikkaus:problem:internal-error` | Unexpected server error | 500 |

`unauthorized` is now reachable: it is what a missing, expired, revoked or malformed credential returns —
see *Authentication* below. `forbidden` is declared and remains unreachable in practice, because MVP 0 has a
single role and an ownership failure deliberately returns `not-found` rather than `forbidden`. Keep handling
it: it is wired up, so a framework-level denial renders as a conforming problem document rather than as
Boot's default body.

A real response body, copied from live output rather than invented:

```json
{
  "detail": "Resource not found.",
  "instance": "/api/v1/does-not-exist",
  "status": 404,
  "title": "Resource not found",
  "type": "urn:rikkaus:problem:not-found",
  "correlationId": "trace-me-0001"
}
```

Two extension members beyond the RFC's standard ones:

- **`correlationId`** — a string, present on every problem body. Same value as the
  `X-Correlation-Id` response header and the server's log lines for that request.
- **`errors`** — an array, present on validation failures only. Each entry is
  `{"field": "...", "message": "..."}`, one per rejected field. Assert against this rather than
  against the status code alone.

`detail` is always safe to show a user. It never carries a stack trace, an internal class name, a SQL
fragment or a credential, and for the statuses where Spring's own text would echo your input it is
replaced with the taxonomy's fixed text. `instance` is the request URI, so it does contain the path you
sent — that is the RFC's definition, not a leak.

## Correlation

Send `X-Correlation-Id` if you want to choose the value. It is echoed when it matches
`^[A-Za-z0-9-]{8,64}$`, and otherwise replaced with a generated UUID. **A rejected value never fails
your request** — you get a working response with a generated identifier instead.

The header is returned on every response, including errors, and it is listed in
`Access-Control-Expose-Headers` on `/api/v1/**` so browser JavaScript can read it. On
`/actuator/health` the header is set but deliberately not exposed to script, because that mapping
allows no custom or exposed headers.

The same identifier is the `correlationId` field of every log line written while the request is
served, and of the one `request completed` line written when it ends — quote it from an error body and
an operator can find the request in the log. What else a log line holds, and what it never holds, is in
[`backend-deployment.md`](backend-deployment.md#log-format-and-what-is-never-logged).

## CORS

`API_ALLOWED_ORIGINS` is a comma-separated allowlist covering `/api/v1/**` and `/actuator/health`.
**Empty permits nothing**: no CORS mapping is registered at all rather than a guessed origin being
allowed, and that is the default in every environment. The `local` profile sets
`http://localhost:8081`, which is the origin Expo web actually serves on.

Credentials are refused. That is the correct posture only because no credential exists yet; enabling
them is a real decision that belongs with the authentication design and must not be inherited from here
as already settled.

## Testing conventions

| Naming | Runner | Needs Docker | What belongs there |
|---|---|---|---|
| `*Test` | Surefire, `mvnw test` | No | Unit tests and `@WebMvcTest` slice tests |
| `*IT` | Failsafe, `mvnw verify` | Yes | Anything needing the real application or database |

An integration test extends
[`AbstractPostgresIntegrationTest`](../services/api/src/test/java/com/rikkaus/wealth/support/AbstractPostgresIntegrationTest.java)
and writes no container boilerplate. The container image tag is pinned to the tag in
`docker-compose.yml`, one container is started for the whole run, and the test profile points at an
unresolvable datasource host so a wiring regression fails loudly instead of running against a
developer's real database.

`verify` therefore fails closed without a Docker daemon. That is intentional. CI must provide Docker
rather than reach for `-DskipITs`.

## Authentication

Identity is **Google Account OIDC only**. There is no password, no sign-up endpoint and no app-managed
credential. That is the accepted product direction (the Product Owner decision recorded in the design for
issue #40), not an unfinished piece.

### The four routes

| Route | Token needed | Body | Success |
|---|---|---|---|
| `POST /api/v1/auth/google` | none | `authorizationCode`, `codeVerifier`, `redirectUri` | 200, a session |
| `POST /api/v1/auth/refresh` | none | `refreshToken` | 200, a new session |
| `POST /api/v1/auth/logout` | none | `refreshToken` | 204 |
| `GET /api/v1/auth/session` | **bearer** | — | 200, the signed-in user |

The client performs the Google handoff and sends the **authorization code**, not an identity token. The
backend redeems it with PKCE, so the Google client secret never reaches the browser and no Google token is
ever held by the client. `redirectUri` must appear in the server's exact-match allowlist or the request is
a 400 with an `errors` entry for `redirectUri`; an unregistered value is never forwarded to Google.

Refresh and logout are deliberately **unauthenticated**. Their credential is the refresh token in the body.
Requiring a live access token would make an expired session impossible to recover from.

A session response:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "refreshToken": "1mQ7rVx0Yb3TkPzFhLdW8sNcJgA6eRuq2ZoXiB4ySvE",
  "user": { "id": "0f2c8d14-...", "email": "an.nguyen@example.com", "displayName": "An Nguyễn" }
}
```

Send the access token as `Authorization: Bearer <accessToken>`. The published contract declares this as the
`bearerAuth` security scheme, applied per route rather than globally, so sign-in and renewal are not
described as needing a token they exist to provide.

### Session lifetimes and rotation

- The access token is a signed JWT valid for **15 minutes**. It carries the user id and session id and
  nothing else — no email, no name.
- The refresh token is valid for **30 days** and is **single-use**. Every renewal returns a new one and
  retires the presented one.
- **Presenting an already-retired refresh token revokes the entire session.** It is treated as evidence
  the value leaked, and by then the successor may already be in someone else's hands. The legitimate user
  is signed out and signs in again.
- Refresh tokens exist in the database only as a SHA-256 hash. An adaptive password hash is deliberately
  not used: the token is 256 random bits, so there is no low-entropy secret for a cost factor to protect.

`GET /api/v1/auth/session` is how a client distinguishes a live session from an expired or revoked one
without inferring it from a failed domain call.

**One limitation, stated plainly:** signing out revokes the ability to renew, not the access token already
issued. The access token is self-contained and verified without a database lookup, so it stays valid for the
remainder of its 15 minutes. `AuthenticationFlowIT` asserts this so it cannot change unnoticed.

### Deny by default — a change from the previous contract

The security chain now denies every route that is not explicitly permitted. Two consequences differ from
the pre-authentication behaviour:

| Request | Was | Is now |
|---|---|---|
| Unauthenticated request to an unknown path under `/api/v1` | 404 `not-found` | **401 `unauthorized`** |
| Any request carrying a malformed or expired bearer token, even to a public route | (not possible) | **401 `unauthorized`** |

The first is deliberate: an unauthenticated caller must not be able to map which paths exist. An
*authenticated* request to an unknown path still returns 404 `not-found`. The second is also deliberate — a
bad token is rejected rather than silently treated as anonymous, so clock skew or a rotated key surfaces as
one clear rejection instead of a confusing success.

The publicly reachable routes are exactly: `GET /api/v1/meta`, the three `POST /api/v1/auth/*` routes above,
`GET /actuator/health`, and — under the `local` profile only — `/v3/api-docs` and `/swagger-ui/**`.
`SecurityConfigurationIT` asserts each one individually, so widening the list requires editing a test.

`unauthorized` and `forbidden` are no longer reserved; `unauthorized` is now reachable. `forbidden` remains
unreachable in practice because MVP 0 has one role and ownership failures return `not-found` — see below.

### Error detail language

Problem `detail` text is English, matching the rest of the taxonomy. The Vietnamese user-facing copy for
each state — expired session, revoked session, cancelled Google handoff, cross-user denial — is specified by
the accepted identity design and is mapped by the client from the `type` URN. Do not parse or display
`detail` as user copy; switch on `type`.

### CORS and credentials

`allowCredentials(false)` stays correct and is not an oversight now that authentication exists. The access
token travels in an `Authorization` header the client sets explicitly, not in a cookie, so no credential is
attached by the browser and none needs to be allowed. That is also why CSRF protection is disabled: there is
no cookie-borne credential to forge. If a cookie session is ever introduced, both decisions must be revisited
together.

## Ownership enforcement — implemented

Every user-owned query and mutation enforces ownership server-side, and the viewer's identity comes only
from the verified access token.

- A missing or invalid principal returns `urn:rikkaus:problem:unauthorized` (401).
- An authenticated principal without rights returns `urn:rikkaus:problem:forbidden` (403). Unreachable in
  MVP 0, which has a single role.
- **A resource owned by another user returns `not-found`, not `forbidden`** — and the response body is
  byte-identical to that of a record that genuinely does not exist. A 403 would confirm the record exists,
  turning any list of identifiers into an enumeration oracle.

Two mechanisms keep this true as slices are added:

- `OwnershipGuard` is the single enforcement point. It takes the detail text from the taxonomy rather than
  from the caller, so no slice can write a helpful-sounding message that reintroduces the disclosure.
- An ArchUnit rule fails the build if any controller method binds a user identifier from a `@PathVariable`
  or `@RequestParam`. `ARCHITECTURE_TECHNOLOGY_DECISIONS.md` requires never trusting a client-supplied
  `userId`; this makes that a compile-time guarantee rather than a review habit.

A later slice inherits the whole rule set by extending `OwnershipContract`, which asserts owner-reads-200,
non-owner-reads-404, non-owner-writes-404, anonymous-401, and the byte-identical-body property.

**What no static rule can guarantee** is that a slice calls `OwnershipGuard` at all. The contract test makes
doing so cheap; it cannot make omitting it impossible.

## Personal profile — implemented

| Method | Path | Body | Returns |
|---|---|---|---|
| `GET` | `/api/v1/profile` | — | `{ id, email, displayName }` |
| `PUT` | `/api/v1/profile` | `{ "displayName": "Trinh Nguyễn" }` | the profile after the change |

The display name is the only editable field. The email comes from the verified Google identity token
and is read-only here, so the request carries no `email` member at all rather than one the server
would have to ignore.

**The name you set survives the next Google sign-in.** `wealth.users` has two name columns.
`display_name` stays whatever Google reports and is still refreshed on every sign-in;
`preferred_display_name` holds what the user typed. `displayName` in every response is the preferred
one when it is set and the Google one otherwise. A single column would have been overwritten the next
time the user signed in, and the edit would have vanished with no error anywhere.

`GET /api/v1/auth/session` reports the same effective name, so the session probe and the profile
resource cannot disagree about what the user is called. **For a client that was reading
`displayName` from the session probe, the field's meaning has changed** even though its shape has not:
it is now the user's chosen name rather than Google's.

`displayName` must be non-blank and at most 120 characters. The bound exists because the column is
`TEXT` and has none of its own; without it the API would accept a megabyte of text into a field the
design renders on one line. The value is trimmed before storing, so a name of nothing but spaces is a
`validation-failed` rather than an empty greeting.

## Exchange-rate settings — implemented

| Method | Path | Body | Returns |
|---|---|---|---|
| `GET` | `/api/v1/settings/exchange-rate` | — | `{ baseCurrency, usdToVndRate, effectiveDate, updatedAt }` |
| `PUT` | `/api/v1/settings/exchange-rate` | `{ "usdToVndRate": 25100.000000, "effectiveDate": "2026-09-12" }` | the settings after the change |

`baseCurrency` is always `VND`. It is stored rather than implied, with a database check that refuses
any other value, so permitting a second reporting currency is a reviewed migration rather than a code
change — but it is not settable and the request carries no member for it.

**Two dates, and they mean different things.** `effectiveDate` is the date the *user* declares the rate
took effect. `updatedAt` is when the *server* stored that declaration. Both are reported, because the
point of the audit requirement is that they are separately visible: "the rate I entered this morning
for last Friday" is a real case.

**All three rate fields are `null` until the user declares a rate**, and the response is still `200`:

```json
{ "baseCurrency": "VND", "usdToVndRate": null, "effectiveDate": null, "updatedAt": null }
```

That is the designed rate-needed state. A `404` would make a user who has never saved look identical
to one who does not exist. The three fields also move together — there is no state where a rate has no
declared date, in the response, in the entity, or in the table.

Validation, each refused as `urn:rikkaus:problem:validation-failed` with the field named:

- `usdToVndRate` must be present and greater than zero.
- `usdToVndRate` must fit `NUMERIC(19, 6)` — at most 13 integer digits and 6 decimal places. Refusing
  it here is what keeps a value PostgreSQL cannot hold from arriving as a 500.
- `effectiveDate` must be present and **must not be in the future**. It is compared against today in
  `Asia/Ho_Chi_Minh`, not UTC: the declared date is a Vietnamese civil date, and judging it in UTC
  would refuse a user who declares today's rate at nine in the evening.

The rate is stored and returned as an exact decimal. A rate of `25100.000000` reads back from the
column as `25100.000000`, unrounded, because `ARCHITECTURE_TECHNOLOGY_DECISIONS.md` forbids binary
floating point for a value that multiplies into every USD holding a user owns.

### Ownership on a singleton resource

Both resources are singletons addressed by the access token: there is no identifier in either path and
no request parameter, so there is nothing a caller could substitute to reach another user. That makes
them the one shape `OwnershipContract` does not fit — it needs a path carrying a record id and a
well-formed path at which no record exists, and neither exists here. `OwnershipGuard.requireOwned` is
still applied to the loaded settings row as a second barrier against a later refactor introducing a
lookup that is not keyed by the viewer.

`GET /api/v1/settings/exchange-rate` creates the default row on a user's first read. It is a write on
exactly one request per user, it is idempotent because the owner is the primary key, and it is done
here rather than at sign-up so the identity slice needs no knowledge that this slice exists — the
ArchUnit slice rule forbids that dependency, and inverting it would mean every new feature slice
editing the sign-in path to seed its own row.

## Asset catalog — implemented

The first domain resource. Five routes, all under `bearerAuth`, all owner-scoped by the token.

| Route | Body | Success |
|---|---|---|
| `GET /api/v1/assets` | — | 200, the viewer's catalog as an array |
| `POST /api/v1/assets` | the six required fields, plus any of the five optional ones | 201, the asset, with `Location` |
| `GET /api/v1/assets/{assetId}` | — | 200, the asset |
| `PUT /api/v1/assets/{assetId}` | the editable metadata | 200, the asset as it now stands |
| `DELETE /api/v1/assets/{assetId}` | — | 204 |

### The eleven categories

`CASH`, `BANK_ACCOUNT`, `TERM_DEPOSIT`, `LISTED_SECURITIES`, `BOND`, `INVESTMENT_FUND`, `REAL_ESTATE`,
`PRIVATE_BUSINESS_EQUITY`, `VEHICLE_OR_VALUABLE_ASSET`, `COLLECTIBLE`, `OTHER_ASSET`.

The same eleven are a check constraint in `V4__assets.sql`, so a twelfth needs a reviewed migration
rather than a code change. No Vietnamese label is published: the client maps each constant to the copy
the accepted design specifies, and a second mapping on the server would be product copy that nothing
keeps in step with the first.

### Fields

| | |
|---|---|
| Required | `name` (≤120 chars), `category`, `currentValue`, `currency`, `valuationDate`, `liquidity` |
| Optional | `purchaseValue`, `purchaseDate`, `quantity`, `institution` (≤160), `notes` (≤2000) |
| Enumerated | `currency` is `VND` or `USD`; `liquidity` is `HIGH`, `MEDIUM` or `LOW` |
| Exact decimals | `currentValue` and `purchaseValue` are `NUMERIC(19, 2)`; `quantity` is `NUMERIC(19, 6)`. The `@Digits` bounds in the request records mirror those columns, so an over-precise value is a named `validation-failed` rather than a 500 from PostgreSQL |

`currentValue` of exactly `0` is accepted — an emptied or fully depreciated holding is a real state.
Negative is refused, because a negative asset is a liability and that is issue #16's own table rather
than a sign convention on this one. `quantity` must be strictly positive when present.

Dates are judged as Vietnamese civil dates in `Asia/Ho_Chi_Minh`, exactly as the declared
exchange-rate date is: `valuationDate` and `purchaseDate` may not be in the future, and `purchaseDate`
may not be later than `valuationDate`. A request with two date problems returns both in one `errors`
array. On `PUT`, the purchase date is judged against the **earliest valuation ever recorded** for the
asset, not only the current one, so a back-dated record in the history can never end up before the
purchase; the message is the same `must not be later than the valuation date`.

### The valuation is not edited; it is recorded

`PUT` accepts `name`, `liquidity`, `purchaseValue`, `purchaseDate`, `quantity`, `institution` and
`notes`. It does **not** accept `currentValue`, `currency`, `valuationDate` or `category`.

That is the accepted design for issue #51 — the current value is read-only in the edit form — and a new
value goes through the valuation history below instead. `currentValue`, `currency` and `valuationDate`
on an asset are a copy of its **current valuation record**, written in the same transaction as that
record; nothing else writes them. Those members are absent from `UpdateAssetMetadataRequest`, and the
only mutator `Asset` has for them takes a stored record rather than loose values.

Creating an asset also writes the first record of its history, so `POST /api/v1/assets` now accepts two
optional members alongside `valuationDate`: `usdToVndRate` and `rateEffectiveDate`, the rate snapshot
for that first record. They obey the rules the history section states, and they are the only change to
the catalog's contract since it shipped.

Two further things about `PUT`:

- **It is a replacement, not a patch.** An optional field omitted from the body is cleared. That is
  what makes "remove my note" expressible without a sentinel value.
- **A frozen member sent anyway is silently dropped**, not rejected. It is an unknown property, and
  Spring Boot's own default is to ignore one. Nothing is applied and nothing is reported, so a client
  must not read a `200` as confirmation that a value it sent was stored.

### No conversion, no totals

An asset reports the amount the user entered and the currency they entered it in. There is no
converted VND figure, no exchange rate and no portfolio total anywhere in this resource.

The rate belongs to the settings resource, which the ArchUnit slice rule forbids the assets slice from
depending on, and the client has already read it from `GET /api/v1/settings/exchange-rate`. Converting
here would also mean storing or recomputing a figure whose rate nobody could see.

### Errors specific to this resource

- An unrecognised `category`, `currency` or `liquidity` is **`malformed-request`, not
  `validation-failed`**, and carries no `errors` array. Jackson cannot construct the request record at
  all, so the failure happens before bean validation runs. Switch on `type`, not on the status.
- A `\u0000` anywhere in `name`, `institution` or `notes` is `validation-failed` naming that field,
  with `must not contain a null character`. PostgreSQL cannot hold that byte in a `text` column, and
  refusing it here is what keeps it a named 400 rather than a 500 from the driver. It is refused, not
  stripped: dropping a character and answering 201 would store something other than what was sent. An
  unpaired surrogate, by contrast, is accepted and stored — that one PostgreSQL takes without
  complaint, so there is no rule against it.
- A `name` that is blank once leading and trailing blank space is removed is `validation-failed` with
  `must not be blank`. **Blank here is wider than `@NotBlank`'s own definition**: it includes every
  Unicode space separator, so a name of a single non-breaking space is refused rather than creating an
  asset that renders as an empty row. The optional text fields are cleaned the same way, and one left
  with nothing becomes `null`.
- Another user's asset identifier answers `not-found`, for reads and for writes, with a body that
  differs from a genuinely missing identifier's only in `instance` and `correlationId`.
- A second `DELETE` of the same identifier answers `not-found` rather than `204`. A deleted identifier
  is treated exactly like one that never existed. **Two deletes that overlap** — what a double-tapped
  confirm button sends — give one `204` and `not-found` to the rest, as does a `PUT` racing a `DELETE`.
  This is explicit rather than incidental: without it the losing request raises a stale-state failure
  that reaches the client as a 500, which it did for 73 of 100 requests when it was measured.
- **There is no row version and no edit-conflict detection.** Two overlapping `PUT`s both succeed and
  the last writer wins, silently. One pilot user on one device makes that acceptable for now;
  introducing a conflict response is a product decision about concurrent editing, not a defect here.
  What a `PUT` can **not** do is roll back a valuation: it takes the same row lock an append takes, so
  an edit and an append that overlap are applied one after the other and the catalog's value stays the
  current record's. This was a review finding, measured as ten rounds of one `PUT` against one `POST`
  with the snapshot correct after every round.
- `GET` on the collection takes **no query parameter**. Search, filter and sort are issue #99's, and
  there is no pagination.

### Verified by hand, not by the suite

**One thing to know before consuming a money field.** `currentValue`, `purchaseValue` and `quantity`
are published as bare JSON numbers. `NUMERIC(19, 2)` permits 17 integer digits, which is past
JavaScript's exact-integer range of about `9.007e15`, so a value above that would be rounded by
`JSON.parse` even though the server and the database hold it exactly. Realistic Vietnamese portfolio
figures are several orders of magnitude below the boundary, so no string encoding was introduced for
it; a client that ever needs the full column range should read these as strings.

No automated test exercises any of these five routes. The resource shipped on Product Owner direction
with manual verification only:
[`docs/manual-test-guide-assets.md`](manual-test-guide-assets.md) is the reproducible procedure and
`plans/reports/manual-verification-261103-2231-GH-53-asset-catalog-common-crud.md` is what it returned
on the implementing run — 70 cases, 64 through HTTP and 6 straight into PostgreSQL.

## Asset valuation history — implemented

Every change to an asset's value is a new record, and no record is ever changed or removed. Three
routes, all under `bearerAuth`, all reached through an asset the viewer owns.

| Route | Body | Success |
|---|---|---|
| `GET /api/v1/assets/{assetId}/valuations` | — | 200, every record, current first |
| `POST /api/v1/assets/{assetId}/valuations` | `value`, `currency`, `effectiveDate`, and optionally `usdToVndRate`, `rateEffectiveDate`, `note`, `correctsValuationId` | 201, the record, with `Location` |
| `GET /api/v1/assets/{assetId}/valuations/{valuationId}` | — | 200, the record |

There is **no `PUT`, `PATCH` or `DELETE`** on either path; each answers `method-not-allowed`. The table
behind them refuses an `UPDATE` and a direct `DELETE` with a trigger, so the rule holds for callers that
are not the API too. The history goes only with its asset: deleting an asset still cascades, exactly as
issue #53's plain delete did.

A record:

```json
{
  "id": "adaafe49-77f5-45d2-b70b-cfcef3871311",
  "assetId": "0422ee6a-d5c0-4922-b881-165401371cb1",
  "value": 21250.00,
  "currency": "USD",
  "effectiveDate": "2026-09-14",
  "usdToVndRate": 25100.000000,
  "rateEffectiveDate": "2026-09-12",
  "note": "Cập nhật số dư sau khi đối chiếu sao kê.",
  "correctsValuationId": null,
  "current": true,
  "recordedAt": "2026-11-14T10:42:38.031962Z"
}
```

### Which record is current

Exactly one record of an asset carries `current: true`: the one with the **latest `effectiveDate`**,
and between two on the same date the **later `recordedAt`**. The list is returned in that order, so the
current record is always first. The asset's `currentValue`, `currency` and `valuationDate` are a copy of
that record's, written in the same transaction, so reading the catalog and reading the history can never
disagree. Appends to one asset are serialised by a row lock, and this was measured: four concurrent
appends, five rounds, the snapshot equal to the marked record every time.

Three consequences a client should rely on:

- **A back-dated record changes nothing but the history.** Posting a value dated before the current
  record's date answers `201` with `current: false`, and the asset is not written — its `updatedAt`
  does not move. This is how last year's values are entered for a trend.
- **A same-day record supersedes.** Two records on one date are a correction and its original; the
  later recorded is current. The design's "Hiện tại" mark is this flag, not a position the client works
  out.
- **The first record is written when the asset is.** An asset always has at least one record, with the
  asset's `createdAt` as its `recordedAt`. Assets that existed before this resource did were given
  theirs by the migration, from their own snapshot, with no rate — the *partial* state below.

### Corrections

To correct a record, post the right value with `correctsValuationId` naming the wrong one and the reason
in `note`. The original stays exactly as it was and stays readable; the new record points at it. A
correction **requires a note** — `note` / `must be given when correcting a valuation` — because the
accepted design makes *Lý do điều chỉnh* mandatory. `correctsValuationId` must name a record **of this
asset**; a missing identifier, a record of another of the viewer's assets and a record of another user's
asset all answer the same `must identify a valuation of this asset`, byte-identical apart from
`correlationId`. The table enforces the same rule with a composite foreign key.

### The rate snapshot

`usdToVndRate` and `rateEffectiveDate` are the rate **the user applied** when recording this value —
the design's *Tỷ giá USD/VND đã dùng* — sent by the client, not read from the settings resource. The
assets slice may not depend on settings, and a stored rate is an audit fact about this record rather than
a pointer to whatever the settings rate later becomes.

- **Both or neither.** A rate without its date is `rateEffectiveDate` / `must be given together with
  usdToVndRate`; a date without its rate is the mirror image.
- **Only on a USD record.** On a VND record both are refused with `must be absent for a VND valuation`.
- **Both may be absent on a USD record.** That is the design's *partial* state, not an error, and it is
  the state of every USD asset created before this resource existed. The client renders it as the
  design's *Thiếu rate metadata* row.
- `usdToVndRate` must be greater than zero and fit `NUMERIC(19, 6)`; `rateEffectiveDate` must not be in
  the future.

There is still **no converted VND figure** anywhere. The client multiplies `value` by `usdToVndRate`
and shows the chain, as the design's reconciliation table does: 21.250 USD × 25.100 = 533.375.000 VND.

### Validation, each a named `validation-failed`

- `value` is required, not negative, at most 17 integer digits and 2 decimals. Zero is accepted.
- `currency` is required and **must be the asset's** — `must match the asset's currency`. A holding's
  currency is a property of the holding and `purchaseValue` is denominated in it. The record still
  stores the currency, as the backbone requires, so relaxing this later needs no migration.
- `effectiveDate` is required, must not be in the future (judged in `Asia/Ho_Chi_Minh`, as every date
  here is) and `must not be earlier than the purchase date` when the asset has one.
- `note` is at most 2000 characters, trimmed of blank space in the wide sense the catalog already uses,
  stored as `null` when nothing is left, and refused with `must not contain a null character` for a
  `\u0000`.
- An unrecognised `currency` is `malformed-request`, as on the catalog.
- Several problems come back in one `errors` array. A VND currency on a USD asset with a future date and
  a rate produced four entries in one response.

### Ownership

Ownership is decided on the asset, by the same guard the catalog uses, and a record is reachable only
through its own asset. Another user's asset answers `not-found` on all three routes with the body a
missing asset produces. A record identifier under the **wrong asset** — another of the viewer's own, or
anybody else's — is `not-found` too, so a valuation identifier is never a way around the ownership of
the asset it belongs to. Seven such bodies were compared in the run and differed only in `instance` and
`correlationId`.

### Verified by hand, not by the suite

No automated test exercises these three routes, on the same Product Owner direction as the catalog.
[`docs/manual-test-guide-asset-valuations.md`](manual-test-guide-asset-valuations.md) is the
reproducible procedure and
`plans/reports/manual-verification-261114-1716-GH-59-asset-valuation-history.md` is what it returned on
the implementing run — 84 cases, 71 through HTTP, 2 concurrency probes and 11 straight into PostgreSQL.

## Liability catalog — implemented

The second domain resource, built on the asset catalog's shape. Five routes, all under `bearerAuth`,
all owner-scoped by the token.

| Route | Body | Success |
|---|---|---|
| `GET /api/v1/liabilities` | — | 200, the viewer's catalog as an array |
| `POST /api/v1/liabilities` | the four required fields, plus any of the nine optional ones | 201, the liability, with `Location` |
| `GET /api/v1/liabilities/{liabilityId}` | — | 200, the liability |
| `PUT /api/v1/liabilities/{liabilityId}` | every field except `category` | 200, the liability as it now stands |
| `DELETE /api/v1/liabilities/{liabilityId}` | — | 204 |

### The six categories

`BANK_LOAN`, `PERSONAL_LOAN`, `MARGIN_LOAN`, `CREDIT_CARD_BALANCE`, `GUARANTEE_OBLIGATION`,
`OTHER_LIABILITY`. The same six are a check constraint in `V7__liabilities.sql`. No Vietnamese label is
published; the client maps each constant to the accepted design's copy.

**The category drives no server-side rule.** The accepted design lets the form offer *không áp dụng*
for the schedule and the maturity only on a credit card balance and a guarantee obligation, and says
in the same table *không tự suy đoán theo danh mục*. That is a rule about what the form shows. Every
term and schedule field below is optional for every category, so a margin loan with no maturity and a
bank loan whose schedule the user has not entered yet are both storable.

### Fields

| | |
|---|---|
| Required | `name` (≤120 chars), `category`, `outstandingBalance`, `currency` |
| Terms, optional | `interestRate`, `startDate`, `maturityDate` |
| Schedule, optional | `recurringPaymentAmount`, `recurringPaymentFrequency`, `nextPaymentDate` |
| Also optional | `collateralAssetId`, `notes` (≤2000) |
| Enumerated | `currency` is `VND` or `USD`; `recurringPaymentFrequency` is `MONTHLY`, `QUARTERLY`, `SEMIANNUAL` or `YEARLY` |
| Exact decimals | `outstandingBalance` and `recurringPaymentAmount` are `NUMERIC(19, 2)`; `interestRate` is `NUMERIC(7, 4)`. The `@Digits` bounds mirror the columns, so an over-precise value is a named `validation-failed` |

`outstandingBalance` of exactly `0` is accepted — a card paid down to nothing is a real state.
Negative is refused. `interestRate` is a **percentage per year** as the user states it, `8.5` meaning
8.5 % per annum; zero is accepted (an interest-free loan), negative is not, and nothing accrues from
it. `recurringPaymentAmount` must be strictly positive when present and is the total due each period,
not split into principal and interest.

**Amounts come back at column scale on every response, including the 201.** A rate sent as `8.5` is
echoed as `8.5000` and a balance sent as `2400000000` as `2400000000.00`, so the create response and a
later read are the same document. (The asset catalog echoes the create body as sent; that difference
is recorded here rather than hidden.)

### Three rules that relate fields

Each is `validation-failed` naming the offending field, and a request that breaks several gets them
all in one `errors` array:

- `maturityDate` must not be before `startDate` — `must not be before the start date`. Equal is allowed.
- `nextPaymentDate` must fall inside the term when the term is given: `must not be before the start
  date`, or `must not be after the maturity date`. It **may be in the past**; an overdue payment is a
  state the design shows, not an input error. With no `startDate` or `maturityDate` it is unconstrained.
- `recurringPaymentAmount` and `recurringPaymentFrequency` come as a pair. One without the other names
  the missing one: `recurringPaymentFrequency: must be present when a recurring payment amount is
  given`, or `recurringPaymentAmount: must be present when a recurring payment frequency is given`.

No date is compared with today. A loan that starts next month is a signed contract, not a mistake.

### The collateral

`collateralAssetId` is the identifier of an asset from `GET /api/v1/assets`, and it **must be the
signed-in user's**. Any other value — another user's asset or an identifier that exists nowhere — is
`validation-failed` with `collateralAssetId: must identify an asset the signed-in user owns`, and the
two cases produce the same body apart from `correlationId`. It is a `validation-failed` and not a
`not-found` because the resource the request addresses, the liability, is fine; one value in the body is
wrong. One message for both cases is deliberate: the alternative would make this field an oracle for
which asset identifiers exist, the disclosure the ownership rule on the path refuses.

The check is re-run on every `PUT`, not only when the link is first made.

**Deleting the pledged asset does not fail and does not delete the liability.** The foreign key is
`ON DELETE SET NULL`: the liability survives and reads back with `collateralAssetId: null`, the same as
one that never had collateral. Refusing the asset's deletion would have changed the asset resource's
behaviour from inside this slice, and the consequences of deleting a linked record are issue #104's. A
client that shows "collateral: Căn hộ Thảo Điền" from its own asset list should expect that name to
disappear when the asset does.

### `PUT` replaces everything but the category

`PUT` accepts every field of the create body except `category`, which is chosen once in the create
flow's picker. The balance and the currency **are** editable here — there is no append-only balance
history for liabilities the way issue #57 introduces one for asset valuations, and the accepted
design's edit screen offers *Dư nợ hiện tại* directly.

- **It is a replacement, not a patch.** An optional field omitted from the body is cleared. Omitting
  `collateralAssetId` unlinks the collateral; omitting `recurringPaymentAmount` and
  `recurringPaymentFrequency` together removes the schedule.
- A `category` sent anyway is silently dropped, as an unknown property, and the stored category is
  unchanged. Do not read a `200` as confirmation that it was applied.

### Listing order

`GET /api/v1/liabilities` returns the catalog **soonest `nextPaymentDate` first**, liabilities with no
next payment date after all of those, and ties broken by `name`. The order is the server's so the
"sắp đến hạn" rows the design surfaces are at the top without the client sorting. No query parameter,
no pagination; search and filter are issue #101's.

### Errors specific to this resource

- An unrecognised `category`, `currency` or `recurringPaymentFrequency` is **`malformed-request`**,
  as on the asset catalog, and a `collateralAssetId` that is not a UUID is `malformed-request` too.
- `\u0000` in `name` or `notes`, and a `name` that is blank once Unicode space separators are
  removed, are refused exactly as the asset catalog refuses them. `notes` is stripped and becomes
  `null` when nothing is left.
- Another user's liability identifier answers `not-found`, for reads and writes, with a body that
  differs from a genuinely missing identifier's only in `instance` and `correlationId`.
- A second `DELETE` answers `not-found`. Overlapping deletes give one `204` and `not-found` to the
  rest, never a 500 — the flush-inside-the-transaction fix from the asset catalog is applied here too.
- There is no row version and no edit-conflict detection, for the reason given under the asset catalog.
- **Nothing is computed.** No amortisation table, no principal and interest split, no interest
  accrual, no VND conversion of a USD balance, no total across liabilities. Those belong to issues
  #74, #77 and #65, and to the client's own use of the declared rate.

### Verified by hand, not by the suite

No automated test exercises any of these five routes. The resource shipped on Product Owner
direction with manual verification only:
[`docs/manual-test-guide-liabilities.md`](manual-test-guide-liabilities.md) is the reproducible
procedure and
`plans/reports/manual-verification-261114-1735-GH-62-liability-catalog-schedules-collateral.md` is
what it returned on the implementing run. The same caveat about money as bare JSON numbers above
JavaScript's exact-integer range applies to `outstandingBalance` and `recurringPaymentAmount`.

## Cash-flow entries — implemented

A domain resource built on the asset and liability catalogs' shape. Five routes, all under `bearerAuth`, all owner-scoped by the token. An entry
is the user's statement of one income or expense and when it applies; a recurring entry is **one row**,
not one row per month.

| Route | Body | Success |
|---|---|---|
| `GET /api/v1/cash-flow-entries` | — | 200, the viewer's entries as an array, newest `effectiveDate` first, then newest `createdAt` |
| `POST /api/v1/cash-flow-entries` | the six required fields, `recurrence` when recurring, plus any of the optional ones | 201, the entry, with `Location` |
| `GET /api/v1/cash-flow-entries/{entryId}` | — | 200, the entry |
| `PUT /api/v1/cash-flow-entries/{entryId}` | the same body as `POST` | 200, the entry as it now stands |
| `DELETE /api/v1/cash-flow-entries/{entryId}` | — | 204 |

### The sixteen categories, tied to their type

Income: `SALARY`, `BUSINESS_INCOME`, `RENTAL_INCOME`, `DIVIDEND`, `DEPOSIT_INTEREST`,
`BOND_OR_FUND_INCOME`, `OTHER_INCOME`. Expense: `LIVING_COSTS`, `HOUSING`, `EDUCATION`, `HEALTHCARE`,
`INSURANCE`, `TAX_AND_FEES`, `DEBT_PRINCIPAL_PAYMENT`, `INTEREST_PAYMENT`, `OTHER_EXPENSE`.

`category` must belong to `entryType`. `SALARY` under `EXPENSE` is a `validation-failed` naming
`category` (`must belong to the entry type`), not a negative salary. The same pairing is a check
constraint in `V8__cash_flow_entries.sql`. No Vietnamese label is published, for the reason the asset
catalog gives.

### Fields

| | |
|---|---|
| Required | `entryType` (`INCOME` \| `EXPENSE`), `category`, `amount`, `currency` (`VND` \| `USD`), `timing` (`ONE_TIME` \| `RECURRING`), `effectiveDate` |
| Conditional | `recurrence` (`MONTHLY` \| `QUARTERLY` \| `YEARLY`): **required** when `timing` is `RECURRING`, **refused** when it is `ONE_TIME` |
| Optional | `endDate` (recurring only), `linkedAssetId` **or** `linkedLiabilityId` (at most one), `notes` (≤2000) |
| Exact decimal | `amount` is `NUMERIC(19, 2)` and must be greater than zero; `@Digits` mirrors the column so an over-precise value is a named `validation-failed`. The response always carries two decimals, so `45000000` reads back as `45000000.00` on the create response and on every later read |

There is **no `name`**. The MVP Backbone lists nine attributes for an entry and a name is not one; the
accepted design's quick-entry form has no field for one. `notes` is where "Lương tháng" goes.

### One-time and recurring

`effectiveDate` is the one date a one-time entry has and the start date of a recurring one. **It may be
in the future**: an expected salary from next January is a legitimate entry and is exactly the input
the three-month forecast needs. This differs deliberately from `valuationDate` on an asset.

A one-time entry carrying `recurrence` or `endDate` is refused, each as its own `errors` entry
(`must be omitted for a one-time entry`). A recurring entry without `recurrence` is refused
(`must be present for a recurring entry`). `endDate` may equal `effectiveDate` — a recurrence of one
occurrence — but may not precede it (`must not be before the effective date`). Omitting `endDate` on a
recurring entry means open-ended.

**Every cross-field problem is reported at once.** A request with three mistakes returns one `errors`
array with three entries, so the form can mark every field in one round trip.

### The optional link

An entry may refer to **one** of the viewer's records: `linkedAssetId` for an asset (rent from an
apartment, dividends from a holding) or `linkedLiabilityId` for a liability (a mortgage's principal or
interest payment). Never both: sending the two together is refused on `linkedLiabilityId` with
`must not be combined with linkedAssetId`, and the table holds the same rule as
`cash_flow_entries_links_at_most_one_record`.

Each identifier must be a record the signed-in user owns. One that belongs to someone else and one that
does not exist both return the **same** `validation-failed` entry — `linkedAssetId` /
`must reference one of the signed-in user's assets`, or `linkedLiabilityId` /
`must reference one of the signed-in user's liabilities` — for the reason ownership failures return
`not-found`: a different answer would confirm which identifiers exist. The response carries the
identifier only, never the linked record's name or balance. The check is a query on the two columns
the foreign key already depends on, because the slice rule forbids `cashflow` importing `assets` or
`liabilities`.

Deleting the linked record does not delete the entry. Both foreign keys are `ON DELETE SET NULL`, the
same choice the liability catalog made for its collateral, so the entry survives with the link set to
`null`. What the user should see when that happens is issue #104's question.

A link is a reference and nothing more. Linking an interest payment to a mortgage does not derive the
amount from the liability's recurring payment, and the liability's own schedule is not turned into
entries; reconciling the two belongs to issues #74 and #77.

### `PUT` replaces everything

The body is the same record as `POST`, and nothing is frozen after creation — unlike an asset's
valuation, no append-only history owns any of these values — so `entryType`, `category` and `amount`
may all change. An optional member omitted from the body is cleared. An unknown member sent anyway is
silently dropped, as on every other route.

### No conversion, no totals, no expansion

An entry reports the amount the user entered and the currency they entered it in. Converting a USD
entry to VND is the client's, with the rate from `GET /api/v1/settings/exchange-rate`. Monthly totals
are issue #77, expanding a recurring entry into its months is issue #74, the forecast is issue #80,
and search or filter parameters are issue #101. `GET` on the collection takes no query parameter.

## Financial goals — implemented

A domain resource. Five routes under `/api/v1/goals` and one read-only route for the
templates, all under `bearerAuth`, all owner-scoped by the token.

| Route | Body | Success |
|---|---|---|
| `GET /api/v1/goals` | — | 200, the viewer's goals as an array, nearest `targetDate` first and then by name |
| `POST /api/v1/goals` | the four required fields, plus any of the three optional ones | 201, the goal, with `Location` |
| `GET /api/v1/goals/{goalId}` | — | 200, the goal |
| `PUT /api/v1/goals/{goalId}` | the same seven fields | 200, the goal as it now stands |
| `DELETE /api/v1/goals/{goalId}` | — | 204 |
| `GET /api/v1/goal-templates` | — | 200, the seven templates with their suggested defaults |

### One model, seven templates

`EMERGENCY_FUND`, `HOME_PURCHASE`, `EDUCATION`, `RETIREMENT`, `NET_WORTH_GROWTH`, `DEBT_REDUCTION`,
`OTHER_GOAL`. The same seven are a check constraint in `V9__goals.sql`.

A template is nothing but a value of `type` plus the defaults the accepted design for issue #84
suggests for a new goal of that type. There is no template table and no per-template field. The
defaults are published so the client does not hard-code product numbers:

```json
{"type":"EMERGENCY_FUND","defaultExpectedAnnualReturnPercent":3.00,"defaultHorizonMonths":18,"amountPrefill":"TARGET_FROM_SIX_MONTHS_OF_EXPENSES"}
```

`amountPrefill` is one of `NONE`, `TARGET_FROM_SIX_MONTHS_OF_EXPENSES`,
`ALLOCATED_FROM_CURRENT_NET_WORTH`, `TARGET_FROM_TOTAL_LIABILITIES`. It **names** the figure the form
should suggest and from what; it never carries the figure. The goals slice may not read the cash-flow,
asset or liability slices, and the client already holds those numbers. The horizon is in months from
today, for the client to turn into a `targetDate`. Every default is a suggestion the user may change.
No Vietnamese label is published; the client maps each constant to the accepted design's copy.

### Fields

| | |
|---|---|
| Required | `name` (≤120 chars), `type`, `targetAmount`, `targetDate` |
| Optional | `currentAllocatedAmount`, `plannedMonthlyContribution`, `expectedAnnualReturnPercent` |
| Read-only | `currency`, always `VND`; `createdAt`, `updatedAt` |
| Exact decimals | the three amounts are `NUMERIC(19, 2)`; the return is `NUMERIC(5, 2)`. The `@Digits` bounds in `GoalRequest` mirror those columns, so an over-precise value is a named `validation-failed` rather than a 500 from PostgreSQL |

Validation, each refused as `validation-failed` with the field named:

- `targetAmount` must be greater than zero.
- `currentAllocatedAmount` must not be negative and **must not exceed `targetAmount`**, the accepted
  design's rule. Equal to the target is accepted. **Omitted, it is stored as `0.00`**, not `null`: a goal
  nothing has been set aside for has zero allocated, and the column is `NOT NULL` so issue #89's
  completion percentage never meets a null.
- `plannedMonthlyContribution` must not be negative. Omitted, it is `null`, and that is deliberately
  **not** the same as `0.00`: the accepted design renders a goal with no stated contribution as "status
  unavailable" and a goal with a stated contribution of zero as a goal that is behind.
- `expectedAnnualReturnPercent` is a percent from `0.00` to `30.00`, the design's bound, with at most two
  decimals. Omitted, it is `null`. It is the user's own assumption and is published as such; nothing in
  the API treats it as a forecast.
- `targetDate` must be **after today** — strictly, as the design says "sau ngày tham chiếu" — judged in
  `Asia/Ho_Chi_Minh` like every other user-entered date. On `PUT`, the rule applies only to a date that
  **differs from the stored one**. Keeping the stored date is always allowed, so a goal whose date has
  passed can still be edited; the issue ticket's "not in the past" wording was resolved towards the
  design's stricter form.
- A `name` that is blank once every Unicode space separator is removed, or that contains `\u0000`, is
  refused exactly as the asset catalog refuses it.

### Everything is editable, the type included

`PUT` replaces all seven fields. Unlike an asset's category, a goal's `type` **can be changed**: no
stored field depends on which template was chosen, so freezing it would protect nothing and would force
a user who picked the wrong template to delete and start over. If the Product Owner wants it frozen,
that is one line in `GoalRequest` and one in `Goal`.

`PUT` is a replacement, not a patch. An optional field omitted from the body is cleared — the allocated
amount to `0.00`, the other two to `null`. A `currency` or any computed member sent anyway is an unknown
property Jackson drops; the response still says `"currency":"VND"`.

### No projection, no conversion

The response carries what the user entered and nothing derived from it. Completion percentage,
remaining amount, remaining months, required monthly contribution and the on-track status are issue
#89's and have no route yet. Publishing them here would fix a projection method in this contract before
that task documents one. Every amount is VND; there is no exchange rate anywhere in this resource.

### Errors specific to this resource

- An unrecognised `type`, or a date that is not ISO 8601, is **`malformed-request`, not
  `validation-failed`**, with no `errors` array. Jackson cannot construct the request record at all.
- Another user's goal identifier answers `not-found`, for reads and writes, with a body that differs from
  a genuinely missing identifier's only in `instance` and `correlationId`.
- A second `DELETE` answers `not-found`. Overlapping deletes give one `204` and `not-found` to the rest,
  never a 500; the slice flushes inside the transaction as the asset slice does.
- There is no row version and no edit-conflict detection; two overlapping `PUT`s both succeed and the
  last writer wins.
- `GET` on the collection takes no query parameter. Search and filter are issue #101's; there is no
  pagination.

### Verified by hand, not by the suite

No automated test exercises any of these six routes. The resource shipped on Product Owner direction
with scenario-first manual verification only:
[`docs/manual-test-guide-goals.md`](manual-test-guide-goals.md) is the reproducible procedure and
`plans/reports/manual-verification-261114-1740-GH-86-generic-financial-goals-templates.md` is what it
returned on the implementing run — 70 cases through HTTP and 11 straight into PostgreSQL.

One contract side effect worth knowing: springdoc derives `operationId` from the handler method name
and suffixes collisions in an order it does not document. Adding the goals controller, whose methods are
also `list`, `create`, `read`, `replace` and `delete`, shifted the suffixes of the asset, valuation and
cash-flow operations by one (`list_2` became `list_3`, `read_4` became `read_5`, and so on) in
`openapi.json`. No client generates code from the contract, so nothing consumes those identifiers; a
future generator should set explicit `operationId`s rather than rely on the derived ones.

## In-product feedback — implemented

Two routes under `/api/v1/feedback`. A pilot user sends feedback from a screen of the app, and reads
back their own; nobody else's is ever returned. Issue #110 shipped it ahead of its design (#108), so the
shape below is the backend's reading of the MVP Backbone and the issue, recorded in
`plans/261114-1843-GH-110-feedback-observability/plan.md` with the questions it leaves for the Product
Owner.

| Route | Body | Answer |
|---|---|---|
| `GET /api/v1/feedback` | — | 200, the viewer's feedback as an array, newest first |
| `POST /api/v1/feedback` | `screen`, `message`, optional `category` | 201, the stored feedback, **no `Location`** |

There is no route that reads, edits or deletes one entry, and no administrative route. Because no
single-entry URI exists, the 201 carries the entry in its body and no `Location` header; `GET` or
`DELETE` on `/api/v1/feedback/{id}` is a plain `not-found`, and `PUT`, `PATCH` or `DELETE` on the
collection is `method-not-allowed`.

### Fields

| Field | Rule |
|---|---|
| `screen` | Required. A route-like code: lowercase ASCII letters and digits in segments joined by `-` or `/`, at most 64 characters — `dashboard`, `catalog/assets`, `goals/new`. Deliberately unable to hold spaces, diacritics, `.`, `?` or `=`, so it cannot carry a sentence, an amount such as `4.200.000.000` or a query string. |
| `message` | Required. Surrounding blank space is removed; blank in the Unicode sense (a lone `U+00A0` included) is refused; at most 2000 characters; `U+0000` is refused rather than becoming a 500. Stored as written. |
| `category` | Optional: `BUG`, `IDEA`, `QUESTION` or `OTHER`. Omitted means `null` — "the user did not say", not `OTHER`. |
| `id`, `createdAt` | Server-assigned, returned only. `createdAt` has microsecond precision, so the create response equals every later read. |

There is no author field in either direction: the author is the access token's user, and a `userId`,
`id` or `createdAt` sent in the body is ignored like any unknown member.

### Errors specific to this resource

- A missing or malformed `screen`, or a missing, blank or over-long `message`, is `validation-failed`
  with every problem in `errors` at once. A blank-to-a-reader message or one carrying `U+0000` is
  reported the same way, after the annotation checks pass.
- An unrecognised `category`, lowercase included, is `malformed-request` with no `errors` array.
- The message is the user's own words and may name an apartment or an amount. It is stored and shown
  back to its author; it is never written to an application log.

### Verified by hand, not by the suite

No automated test exercises these routes, on Product Owner direction.
[`docs/manual-test-guide-feedback-observability.md`](manual-test-guide-feedback-observability.md) is the
reproducible procedure, including the log checks, and
`plans/reports/manual-verification-261114-1843-GH-110-feedback-observability.md` is what it returned on
the implementing run. The handler methods are named `listFeedback` and `sendFeedback` rather than
`list` and `send`, so the derived `operationId`s of the other resources did not shift.

## What is not here

Named explicitly, so nothing above is read as a guarantee it does not make.

- CI pipeline, coverage thresholds and formatting enforcement. JSON log encoding now exists outside
  the `local` profile (issue #110); log-based metrics, dashboards and alerting do not.
- Pagination conventions, rate limiting and idempotency keys. The asset catalog returns an unpaginated
  array; a pilot user's catalog is tens of rows, and committing the frontend to a page shape nobody has
  agreed is worse than returning all of them.
- **The remaining domain resources.** Assets, liabilities, cash-flow entries, goals and feedback are
  described above. Insights are still later work.
- **Recurring-entry expansion, monthly totals and the forecast.** Issues #74, #77 and #80. A recurring
  cash-flow entry is one row and the API never says which months it falls in.
- **Type-specific asset fields.** Symbol or instrument code, cost per unit, current price per unit and
  interest rate or expected yield are issue #54's. The asset table has no column for any of them,
  deliberately: a nullable column nobody writes reads as a promise.
- **Editing or deleting a valuation.** The history is append-only by design and by trigger. A wrong
  record is corrected by a new one that points at it; there is no route and no table path that changes
  or removes a record while its asset exists.
- **A converted VND figure on a valuation record, and any freshness judgement about its rate.** The
  record publishes the value and the rate the user applied; the arithmetic and the staleness rule are
  the client's, per the accepted designs for issues #48 and #57.
- **Hardened deletion.** `DELETE` removes the row. There is no undo, no soft delete, no retention
  window and no handling of records that will later reference an asset. Issue #102 owns all of it for
  assets and issue #104 for goals.
- **Goal projections.** No completion percentage, remaining amount, remaining time, required monthly
  contribution or status is computed or published anywhere. Issue #89 owns the projection method and
  its documentation; the goals resource is shaped so it can add them without reopening this contract.
- **Search, filter and sort over the catalog.** Issue #99's.
- Account deletion, a second identity provider, household or shared access, and roles.
- **Exchange-rate history.** Declaring a rate overwrites the previous one. There is no change log, no
  history resource, no automatic or fetched rate, and no third currency.
- **No stale-rate rule.** The API reports the declared date and the stored timestamp and makes no
  freshness judgement. There is no N-day threshold anywhere in the backend; deciding what counts as
  out of date is the client's, per the accepted design for issue #48.
- **Automated tests for the profile, exchange-rate, asset, valuation, cash-flow and goal routes.** All
  six resources shipped on Product Owner direction with manual verification only, recorded in
  `plans/reports/manual-verification-261101-1320-GH-50-profile-exchange-rate-settings.md`,
  `plans/reports/manual-verification-261103-2231-GH-53-asset-catalog-common-crud.md`,
  `plans/reports/manual-verification-261114-1716-GH-59-asset-valuation-history.md` and
  `plans/reports/manual-verification-261114-1735-GH-71-cash-flow-entry-catalog-crud.md` and
  `plans/reports/manual-verification-261114-1740-GH-86-generic-financial-goals-templates.md`. Every other route
  named in this document is covered by the suite. What the suite still enforces across all of them:
  `ApplicationContextIT` fails on a table no migration declares, `OpenApiContractIT` fails on a
  published path that is not declared or a declared one that is not published, and
  `ArchitectureRulesTest` fails on a controller binding an owner identifier from the request, on a
  monetary `double`, and on one feature slice reaching into another.
- **`/actuator/health` is absent from the published contract**, by design, because the contract is
  scoped to `/api/v1/**`. The endpoint works and is CORS-enabled; it simply is not described there.
- **Contract consumption is hand-written this iteration.** `apps/mobile` has no OpenAPI codegen
  dependency and no generate script, so this document and `openapi.json` are review and documentation
  artifacts, not a generation input. If codegen is wanted, it belongs to the frontend task's scope.
