# Manual verification — typed API client foundation (#38)

All 30 test cases in
[`plans/261122-1629-GH-38-typed-api-client/test-cases.md`](../261122-1629-GH-38-typed-api-client/test-cases.md)
pass. 27 ran against the real backend or the real Expo web page. 3 ran on fixture bodies because no
route can produce the response (listed below). The run also found two backend gaps; neither was
patched here.

Run on 2026-11-22 (Asia/Saigon) against `main` at `da67680`:

- API: `services/api` started with `spring-boot:run`, profile `local`, port 8180, allowed origin
  `http://localhost:8091`, on the shared `docker compose` Postgres (`rikkaus_wealth_os-postgres-1`),
  which was reused rather than duplicated. JDK 25.
- Web: Expo web on port 8091 with `EXPO_PUBLIC_API_BASE_URL=http://localhost:8180`, opened in the in-app
  browser at desktop and 375×812 sizes.
- Identity: one row, `c3800000-0038-4038-8038-000000000038`, inserted into `wealth.users`. Tokens were
  minted as `docs/manual-test-guide-assets.md` §4 describes. The row was deleted afterwards
  (`DELETE 1`; no dependent rows existed).
- All processes started for the run (API, Expo) were stopped, and ports 8180 and 8091 are free.

## Evidence

| File | What it holds |
|---|---|
| [`gh38-evidence/harness-results.json`](gh38-evidence/harness-results.json) | Each harness case with its checks and every request/response pair: method, URL, headers with tokens redacted, status, `X-Correlation-Id`, and body |
| [`gh38-evidence/run-test-cases.mts`](gh38-evidence/run-test-cases.mts) | The harness. Run it with `npx tsx run-test-cases.mts apps/mobile <out.json>`, with `API` and `USER_ID` set. It imports `apps/mobile/src/api` unchanged |
| [`gh38-evidence/browser-cors-correlation.json`](gh38-evidence/browser-cors-correlation.json) | `fetch` results from the page at origin 8091 |
| [`gh38-evidence/static-tc-28-29.txt`](gh38-evidence/static-tc-28-29.txt) | Output of the contract-drift and schema-drift checks |
| `gh38-evidence/tc-*.jpg` | Screenshots of the diagnostics screen |

## Results

| Case | Result | Source | Key observation |
|---|---|---|---|
| TC-01 base URL with trailing slash | Pass | Real | Request URL `http://localhost:8180/api/v1/meta` |
| TC-02 missing/invalid base URL | Pass | Harness and browser | Unset, `not-a-url` and `ftp://…` each give `configuration` with no network call. The page shows `Chưa cấu hình địa chỉ API.` (`tc-02-diagnostics-unconfigured-mobile.jpg`) |
| TC-03 meta succeeds | Pass | Real | `apiVersion: v1`, `serverTime` ends in `Z`, correlation id present |
| TC-04 bearer token from the provider | Pass | Real | `GET /api/v1/auth/session` returns 200 with `id` equal to the inserted user |
| TC-05 no token on public/operational routes | Pass | Real | With a forged token in the provider, meta and health are still 200, and no `Authorization` header was sent |
| TC-06 diagnostics success | Pass | Browser | `API phản hồi bình thường. Trạng thái: UP` (`tc-06-*.jpg`, desktop and mobile) |
| TC-07 `validation-failed` | Pass | Real 400 | `fields` holds the six required fields. No server `message` appears in the copy |
| TC-08 `malformed-request` | Pass | Real 400 | Body `{` |
| TC-09 `not-found` | Pass | Real 404 | Shows `Không thể mở mục này` |
| TC-10 `method-not-allowed` | Pass | Real 405 | `DELETE /api/v1/meta` |
| TC-11 `not-acceptable` | Pass | Real 406 | `Accept: text/csv` |
| TC-12 `unsupported-media-type` | Pass | Real 415 | `Content-Type: text/plain` |
| TC-13 `unauthorized` | Pass | Real 401 | Covers no token and an expired token. `requiresSignIn: true` |
| TC-14 `forbidden` | Pass | **Fixture** | Unreachable in MVP 0: there is one role, and ownership failures return 404 |
| TC-15 `internal-error` | Pass | **Real 500** | Produced by a correctly signed token whose user has no row, on `POST /api/v1/assets` (see gap 2) |
| TC-16 unknown future URN | Pass | Fixture | A 429 `rate-limited` falls back to `Yêu cầu chưa hoàn tất` |
| TC-17 401 → refresh → retry once | Pass | Real | One `onUnauthorized` call, statuses `[401, 200]` |
| TC-18 no second retry | Pass | Real | Exactly 2 requests, then the 401 surfaces |
| TC-19 `'fail'` decision | Pass | Real | Exactly 1 request |
| TC-20 correlation echo | Pass | Real and browser | The generated UUID is echoed, and the header is readable cross-origin |
| TC-21 caller id on errors | Pass | Real and browser | `gh38-trace-0001` appears in the header, the body and `ApiError`. Label: `Mã tham chiếu: gh38-trace-0001` |
| TC-22 no custom header on health | Pass | Browser | Health is 200 without one. The same request **with** `X-Correlation-Id` is blocked by the preflight, which confirms why the operational route omits it |
| TC-23 network failure and recovery | Pass | Real and browser | Shows `Không kết nối được máy chủ`, with the offline icon and **Thử lại**. After the API restarted, **Thử lại** returned to success (`tc-23-*.jpg`) |
| TC-24 schema mismatch | Pass | Real 200 | `issuePaths: ['apiVersion']`, with the real correlation id kept |
| TC-25 non-problem error body | Pass | **Fixture** | Actuator `503 {"status":"DOWN"}`. The shared database was not stopped to provoke it |
| TC-26 abort | Pass | Real | The `AbortError` propagates unchanged |
| TC-27 retry policy | Pass | Real errors | Retries only network, 5xx and `internal-error`, at most once |
| TC-28 generated output in sync | Pass | Static | `api:check` passes. A temporary contract edit fails it. The contract was restored and `git status` is clean |
| TC-29 schema drift fails typecheck | Pass | Static | Changing `apiVersion` to `z.number()` fails `tsc`. Reverted |
| TC-30 contract fixtures | Pass | Harness | 51 examples. Every `problem+json` example parses and maps to a known URN. The meta fixture parses through the client |

Quality gates in `apps/mobile`: `npm run lint`, `npm run typecheck`, `npm run api:check` and
`npm run build:web` all pass, and `npm ci` is clean with the new lockfile. The existing Jest suite
also passes (46 suites, 513 tests). The two health test files were edited only where they referenced
the removed `HealthCheckError` and `normalizeApiBaseUrl`, or asserted English error text. No new unit
test was added.

## Backend gaps found (not patched)

1. **CORS on `/api/v1/**` does not allow the `Authorization` header.**
   `services/api/src/main/java/com/rikkaus/wealth/shared/api/CorsConfiguration.java:56` allows only
   `Content-Type` and `X-Correlation-Id`. A preflight asking for `authorization` gets `403` with no
   `Access-Control-Allow-*` headers, and the browser fetch fails as `TypeError: Failed to fetch`.
   Every authenticated call from Expo web is therefore blocked. Native is unaffected. The client
   classifies this correctly as `network` / `Không kết nối được máy chủ`, but no client change can fix
   it. It must land with the #41 backend change, before #41's web sign-in can work. Reproduction is in
   the shared session notes and in `browser-cors-correlation.json` (`gap_authorizationHeader`).
2. **A valid token for a user with no row returns 500 on writes.** `POST /api/v1/assets` with a
   correctly signed token whose `sub` has no `wealth.users` row returns
   `urn:rikkaus:problem:internal-error`, which is an FK failure. `GET /api/v1/auth/session` with the
   same token correctly returns 401. This is low risk, because tokens are only issued for existing
   users. A deleted account with a live token would hit it.

## Not covered

- One change was made after the run. An abort that arrives while a response body is still being read
  now propagates as cancellation; before the change it was reported as `schema-mismatch`. Lint,
  typecheck and the health tests were rerun after it. The harness was not rerun, because TC-26
  aborts before any response.

- Native iOS/Android runtime. The client uses only `fetch`, `Headers`, `URL` and `URLSearchParams`.
  It falls back from `crypto.randomUUID` when that is absent. It was not run on a device in this slice.
- `forbidden` and Actuator `DOWN` against the real server, for the reasons given above.
