# Test cases — typed API client foundation (#38)

Written before the implementation. Every case is executed against the real backend
(`services/api`, profile `local`, port 8180, the shared `docker compose` Postgres) unless its
**Run** line says otherwise, and the result is recorded in
`plans/reports/manual-verification-261122-1629-GH-38-typed-api-client.md`.

No unit test is written for this slice, by Product Owner direction. These cases are the
verification.

## How the cases are run

| Surface | What it proves |
|---|---|
| **Harness** — a script run with `npx tsx` that imports `apps/mobile/src/api` unchanged and calls the real API | The client's behaviour on real responses: parsing, classification, correlation, auth hook |
| **Browser** — Expo web on `http://localhost:8091`, driven through the in-app browser | CORS, readable response headers, and the migrated diagnostics screen as a user sees it |
| **Static** — `npm run typecheck` | Compile-time contract drift between Zod schemas and generated OpenAPI types |

Tokens are minted exactly as `docs/manual-test-guide-assets.md` §4 describes, for one user row
inserted into the shared database (and removed at tear-down).

Copy is asserted exactly. `detail` from the server is never displayed; each case checks that the
Vietnamese text comes from the `type` URN.

## Configuration

### TC-01 — base URL is resolved from `EXPO_PUBLIC_API_BASE_URL`
- **Given** `EXPO_PUBLIC_API_BASE_URL=http://localhost:8180/` (trailing slash)
- **When** the client requests `GET /api/v1/meta`
- **Then** the request goes to `http://localhost:8180/api/v1/meta` (no double slash)
- **Run:** Harness

### TC-02 — missing or invalid base URL fails before any request
- **Given** the variable is unset, then `not-a-url`, then `ftp://localhost:8180`
- **When** a request is made
- **Then** it throws `ApiError` of kind `configuration`, no network call is made, and the
  classifier returns title `Chưa cấu hình đúng địa chỉ API`
- **Run:** Harness (unset and invalid) and Browser (diagnostics screen with the variable unset
  shows `Chưa cấu hình địa chỉ API.` empty state, unchanged from today)

## Success

### TC-03 — `GET /api/v1/meta` succeeds and is validated
- **Given** the API is running
- **When** the client requests meta with `MetaResponseSchema`
- **Then** it resolves `{ data, status: 200, correlationId }` where `data.apiVersion === 'v1'`,
  `data.serverTime` is an ISO-8601 string ending in `Z`, and `correlationId` is non-empty
- **Run:** Harness

### TC-04 — authenticated success attaches the bearer token from the provider
- **Given** a minted token for the inserted user and an auth hook whose `getAccessToken` returns it
- **When** the client requests `GET /api/v1/auth/session`
- **Then** it resolves 200 and `data.id` (the contract's `AuthenticatedUserResponse`) equals the
  inserted user id; the request carried
  `Authorization: Bearer …`
- **Run:** Harness

### TC-05 — public and operational routes do not carry the token
- **Given** an auth hook that returns a **forged** token
- **When** the client requests `GET /api/v1/meta` with `auth: false`, and `GET /actuator/health`
- **Then** both succeed (a forged token on any route would be a 401, so success proves the header
  was not sent)
- **Run:** Harness

### TC-06 — diagnostics screen shows a healthy API through the new client
- **Given** Expo web on 8091 pointed at the API on 8180
- **When** the diagnostics screen (`/diagnostics`) loads
- **Then** it shows `API phản hồi bình thường. Trạng thái: UP` and the **Kiểm tra lại** action
- **Run:** Browser (screenshot)

## Problems — one per `type` URN

Each case asserts: `ApiError.kind === 'problem'`, the exact `problem.type`, the HTTP status, a
non-empty `correlationId` equal to the body's `correlationId`, and the classifier's Vietnamese
title and message.

| ID | `type` | How it is provoked against the real API | Expected title | Retryable | Needs sign-in |
|---|---|---|---|---|---|
| TC-07 | `validation-failed` | `POST /api/v1/assets` with token and body `{}` | `Thông tin chưa hợp lệ` | no | no |
| TC-08 | `malformed-request` | `POST /api/v1/assets` with token and body text `{` | `Yêu cầu không đọc được` | no | no |
| TC-09 | `not-found` | `GET /api/v1/assets/{random UUID}` with token | `Không thể mở mục này` | no | no |
| TC-10 | `method-not-allowed` | `DELETE /api/v1/meta` | `Ứng dụng cần được cập nhật` | no | no |
| TC-11 | `not-acceptable` | `GET /api/v1/meta` with `Accept: text/csv` | `Ứng dụng cần được cập nhật` | no | no |
| TC-12 | `unsupported-media-type` | `POST /api/v1/assets` with token and `Content-Type: text/plain` | `Ứng dụng cần được cập nhật` | no | no |
| TC-13 | `unauthorized` | `GET /api/v1/auth/session` with no token, then with an expired token | `Phiên đăng nhập đã hết hạn` | no | **yes** |
| TC-14 | `forbidden` | **Unreachable in MVP 0** (single role; ownership returns 404). Run through the contract-derived fixture body instead and recorded as such | `Không thể mở mục này` | no | no |
| TC-15 | `internal-error` | Attempted with a token whose subject has no user row on a write route; if the API answers otherwise, run through a fixture body and record the gap | `Máy chủ gặp sự cố` | yes | no |

### TC-07a — validation errors expose field names, never server text
- **Given** TC-07's response
- **When** it is classified
- **Then** `fields` lists the rejected field names from `errors[].field`, and no string from
  `errors[].message` or `detail` appears in the presentation's `title` or `message`

### TC-16 — an unknown future `type` URN degrades by status
- **Given** a problem body with `type: urn:rikkaus:problem:rate-limited` and status 429 (fixture,
  because no route returns it)
- **When** it is classified
- **Then** kind stays `problem`, title is `Yêu cầu chưa hoàn tất`, and nothing throws
- **Run:** Harness with fixture fetch

## Auth interceptor hook

### TC-17 — 401 calls `onUnauthorized` once and retries once with a fresh token
- **Given** `getAccessToken` returns an expired token on the first call and a valid one on the
  second, and `onUnauthorized` returns `'retry'`
- **When** the client requests `GET /api/v1/auth/session`
- **Then** `onUnauthorized` is called exactly once with the `unauthorized` problem and its
  correlation id, the second request succeeds with 200, and two requests reached the server
- **Run:** Harness

### TC-18 — a second 401 is not retried again
- **Given** `getAccessToken` always returns an expired token and `onUnauthorized` returns `'retry'`
- **When** the client requests `GET /api/v1/auth/session`
- **Then** it rejects with the `unauthorized` problem, `onUnauthorized` was called once, and
  exactly two requests reached the server (no loop)
- **Run:** Harness

### TC-19 — `onUnauthorized` returning `'fail'` surfaces the problem without a retry
- **Run:** Harness; expect one request and the `unauthorized` problem

## Correlation

### TC-20 — the client sends a valid correlation id and the server echoes it
- **When** the client requests meta
- **Then** the sent `X-Correlation-Id` matches `^[A-Za-z0-9-]{8,64}$` and the response header
  carries the same value
- **Run:** Harness and Browser (read through `fetch` from the 8091 origin, proving the header is
  CORS-exposed and the preflight accepts the custom header)

### TC-21 — a caller-chosen id is echoed on errors, in header and body alike
- **Given** the caller passes `correlationId: 'gh38-trace-0001'`
- **When** the request is `GET /api/v1/assets/{random UUID}` (404)
- **Then** `ApiError.correlationId`, the response header and the body `correlationId` are all
  `gh38-trace-0001`, and the classifier exposes it for display as `Mã tham chiếu: gh38-trace-0001`

### TC-22 — `/actuator/health` gets no custom header (its CORS mapping allows none)
- **Then** the health request in the Browser succeeds without a CORS preflight failure

## Transport and shape failures

### TC-23 — network failure
- **Given** a base URL whose port has no listener (`http://localhost:8199`), and separately the
  API stopped while the diagnostics screen is open
- **When** a request is made
- **Then** it throws kind `network`; title `Không kết nối được máy chủ`, retryable yes; the
  diagnostics screen shows the error state with **Thử lại**, and after the API is started again
  **Thử lại** returns to the success state
- **Run:** Harness and Browser (screenshots before and after recovery)

### TC-24 — schema mismatch on a 2xx body
- **Given** a real `GET /api/v1/meta` 200 response
- **When** it is parsed with a schema that requires a field the contract does not have
  (`z.object({ apiVersion: z.literal('v2') })`)
- **Then** it throws kind `schema-mismatch` with status 200 and the real correlation id; title
  `Dữ liệu trả về không đúng định dạng`, retryable no
- **Run:** Harness

### TC-25 — a non-problem error body is still classified
- **Given** an `/actuator/health` response that is not 2xx (fixture: 503 `{"status":"DOWN"}`,
  the Actuator shape, because the real database must not be stopped)
- **Then** kind `http`, status 503, title `Máy chủ chưa sẵn sàng`, retryable yes
- **Run:** Harness with fixture fetch

### TC-26 — aborting a request is not reported as a network failure
- **When** a request's `AbortSignal` is aborted before the response
- **Then** the original `AbortError` propagates unchanged (TanStack Query cancellation relies on it)
- **Run:** Harness

## TanStack Query integration

### TC-27 — retry policy follows the classification
- **Then** `shouldRetryApiError(0, error)` is true for `network`, `internal-error` and 5xx `http`,
  and false for every 4xx problem, `schema-mismatch` and `configuration`; never more than one
  retry
- **Run:** Harness against errors produced by TC-07…TC-24

## Contract sync

### TC-28 — generated types match the committed contract
- **When** `npm run api:check` runs
- **Then** it regenerates types and fixtures from `services/api/openapi/openapi.json` and exits 0
  with no diff; after a temporary edit to a copy of the contract it exits non-zero
- **Run:** Static

### TC-29 — a Zod schema that drifts from the contract fails typecheck
- **Given** `MetaResponseSchema.apiVersion` temporarily changed to `z.number()`
- **Then** `npm run typecheck` fails at the request call site; reverted afterwards
- **Run:** Static

### TC-30 — every contract example is available as a fixture and passes its schema
- **Then** the fixture map holds every named example in the contract (51 today, across 31
  operation/status pairs), and the
  `GET /api/v1/meta 200` example parses with `MetaResponseSchema`; every `problem+json` example
  parses with `ProblemDetailSchema` and classifies without falling back
- **Run:** Harness
