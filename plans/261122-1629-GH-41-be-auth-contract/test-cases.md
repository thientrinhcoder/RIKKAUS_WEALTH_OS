# Test cases — GH-41 backend auth contract (web refresh cookie + local dev sign-in)

Written before implementation. No unit tests (project rule); every case is executed with `curl` against
the real API (`local` profile, port 8380) and the root `docker compose` Postgres. Evidence goes to
`plans/reports/`.

## Contract under test

- **Web mode** is signalled by the request header `X-Rikkaus-Client: web`. Without it a request is
  **native mode** and behaves exactly as before.
- Web mode on `POST /auth/google`, `/auth/refresh`, `/auth/dev-sign-in`: the refresh token is set as cookie
  `rikkaus_refresh` (HttpOnly, Secure unless `rikkaus.auth.refresh-cookie.secure=false` as in the `local`
  profile, SameSite=Strict, Path=`/api/v1/auth`, Max-Age=2592000) and is **absent** from the JSON body.
- `/auth/refresh` and `/auth/logout` use the body `refreshToken` when present; otherwise the cookie.
- Using the cookie requires `X-Rikkaus-Client: web` (CSRF: a custom header cannot be sent cross-site
  without a CORS preflight). Any web-mode request whose `Origin` is present and not on the allowlist is
  refused. Refusals are 403 `urn:rikkaus:problem:forbidden`.
- `/auth/logout` always answers with a cookie-clearing `Set-Cookie` (Max-Age=0).
- CORS on `/api/v1/auth/**`: exact allowlisted origins, `Access-Control-Allow-Credentials: true`, allowed
  headers include `X-Rikkaus-Client` and `Authorization`. Other `/api/v1/**`: no credentials.
- `POST /api/v1/auth/dev-sign-in` exists only under the `local` profile and is absent from the committed
  OpenAPI document.

## Cases

| ID | Case | Request | Expected |
|---|---|---|---|
| TC-01 | Dev sign-in, web mode, new user | `POST /auth/dev-sign-in` + `X-Rikkaus-Client: web`, `Origin: http://localhost:8081`, body `{"email":"an.nguyen@example.com","displayName":"An Nguyễn"}` | 200; body has `accessToken`, `tokenType`, `expiresInSeconds: 900`, `user`; **no** `refreshToken`; `Set-Cookie: rikkaus_refresh=…; Path=/api/v1/auth; Max-Age=2592000; HttpOnly; SameSite=Strict` (no `Secure` under local); a `wealth.users` row exists with subject `local-dev\|an.nguyen@example.com` |
| TC-02 | Dev sign-in again, same email | repeat TC-01 | 200; same `user.id`; no second users row |
| TC-03 | Access token works | `GET /auth/session` with bearer from TC-01 | 200, email `an.nguyen@example.com` |
| TC-04 | Rotation via cookie | `POST /auth/refresh`, web header, cookie from TC-01, no body | 200; new `accessToken`; no body `refreshToken`; new `Set-Cookie` value differs from TC-01 |
| TC-05 | Reuse of a rotated cookie revokes the session | `POST /auth/refresh`, web header, **old** cookie from TC-01 | 401 `unauthorized`; then refresh with TC-04's (newer) cookie → 401 too (whole session revoked); DB shows chain revoked |
| TC-06 | Logout via cookie clears the cookie | fresh dev sign-in; `POST /auth/logout`, web header, cookie, no body | 204; `Set-Cookie: rikkaus_refresh=; Max-Age=0; …Path=/api/v1/auth`; refresh with that cookie → 401 |
| TC-07 | CSRF: cookie without the web header | `POST /auth/refresh`, cookie only, no header, no body | 403 `forbidden`; token **not** consumed (a following proper web refresh succeeds) |
| TC-08 | CSRF: foreign Origin | `POST /auth/refresh`, web header, cookie, `Origin: https://evil.example` | 403 `forbidden`; token not consumed |
| TC-09 | Logout CSRF: cookie without header | `POST /auth/logout`, cookie only | 403 `forbidden`; session still renewable |
| TC-10 | Neither body token nor cookie | `POST /auth/refresh` web header, body `{}`; and `POST /auth/logout` with `{}` | 400 `validation-failed`, `errors[0].field = refreshToken` |
| TC-11 | CORS preflight, allowed origin, auth route | `OPTIONS /auth/refresh`, `Origin: http://localhost:8081`, `Access-Control-Request-Method: POST`, `Access-Control-Request-Headers: content-type,x-rikkaus-client` | 200; `Access-Control-Allow-Origin: http://localhost:8081`; `Access-Control-Allow-Credentials: true`; allowed headers include `x-rikkaus-client` |
| TC-12 | CORS preflight, unknown origin | same with `Origin: https://evil.example` | 403 `Invalid CORS request`; no `Access-Control-Allow-Origin` |
| TC-13 | CORS on a non-auth route stays credential-free | `OPTIONS /api/v1/assets`, allowed origin, request headers `authorization` | 200; `Access-Control-Allow-Origin` echoes origin; **no** `Access-Control-Allow-Credentials`; `authorization` allowed |
| TC-14 | Native body flow unchanged | dev sign-in **without** web header; refresh with body `refreshToken`; logout with body | sign-in 200 with body `refreshToken` and no `Set-Cookie: rikkaus_refresh=<value>`; refresh 200 with new body token, no cookie; logout 204 |
| TC-15 | Native reuse still revokes | refresh twice with the same body token | second → 401; successor token → 401 |
| TC-16 | Dev sign-in validation | body `{"email":"not-an-email"}` | 400 `validation-failed` with `errors` for `email` |
| TC-17 | Dev sign-in absent outside `local` | run the same jar with the `dev`-like profile set (no `local`), `POST /auth/dev-sign-in` unauthenticated and with a valid bearer | unauthenticated 401 (deny by default, indistinguishable from any unknown path); authenticated 404 `not-found` |
| TC-18 | Dev sign-in absent from the published contract | `grep dev-sign-in services/api/openapi/openapi.json` | no match; `openapi.json` documents the cookie, `X-Rikkaus-Client`, and optional `refreshToken` |
| TC-19 | Google sign-in route still validates | `POST /auth/google` web header, body `{}` | 400 `validation-failed` (route reachable; real Google not available locally) |
| TC-20 | Build gate | `./mvnw verify` | BUILD SUCCESS |
