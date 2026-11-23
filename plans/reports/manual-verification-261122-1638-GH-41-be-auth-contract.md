# Manual verification — GH-41 backend auth contract (web refresh cookie + local dev sign-in)

Run on 2026-11-22 (Asia/Saigon) against the real API jar and the root `docker compose` Postgres
(`rikkaus_wealth_os-postgres-1`, fresh volume, Flyway V1–V3 applied on start; no new migration, so no
backup was needed). The `local` run listened on port 8380; the no-profile run of the same jar on 8381.
Cases are defined in [`test-cases.md`](../261122-1629-GH-41-be-auth-contract/test-cases.md) and executed by
[`run-auth-cases.sh`](../261122-1629-GH-41-be-auth-contract/run-auth-cases.sh). Token and cookie values are
redacted; a fingerprint `<len sha>` shows when a value changed.

## Result

All 20 cases pass.

| ID | Result | Evidence |
|---|---|---|
| TC-01 | Pass | 200, body without `refreshToken`, `Set-Cookie: rikkaus_refresh=…; Path=/api/v1/auth; Max-Age=2592000; HttpOnly; SameSite=Strict`, row `local-dev\|an.nguyen@example.com` |
| TC-02 | Pass | same `user.id`, one row |
| TC-03 | Pass | `GET /session` 200 |
| TC-04 | Pass | 200, cookie value changed (`d2e18141` → `fa221e87`) |
| TC-05 | Pass | old cookie 401, newer cookie 401, chain `2 tokens, 2 revoked` |
| TC-06 | Pass | 204 with `Max-Age=0`, then 401 |
| TC-07 | Pass | 403 `forbidden`; next proper refresh 200 |
| TC-08 | Pass | 403 `Invalid CORS request`; next proper refresh 200. Without any CORS mapping (no-profile run) the application check answers 403 `forbidden` (TC-08b) |
| TC-09 | Pass | 403; session still renewable |
| TC-10 | Pass | 400 `validation-failed`, field `refreshToken` |
| TC-11 | Pass | 200, `Allow-Origin: http://localhost:8081`, `Allow-Credentials: true`, `x-rikkaus-client` allowed |
| TC-12 | Pass | 403, no `Allow-Origin` |
| TC-13 | Pass | 200, no `Allow-Credentials`, `authorization` allowed |
| TC-14 | Pass | body `refreshToken` present, no cookie on sign-in/refresh, rotation, logout 204 |
| TC-15 | Pass | reuse 401, successor 401 |
| TC-16 | Pass | 400, field `email` |
| TC-17 | Pass | no profile: anonymous 401, with valid bearer 404 `not-found` |
| TC-18 | Pass | `grep -c dev-sign-in services/api/openapi/openapi.json` → 0; contract documents `X-Rikkaus-Client`, cookie `rikkaus_refresh`, optional `refreshToken`, 403 responses |
| TC-19 | Pass | 400 `validation-failed` |
| TC-20 | Pass | `mvnw verify -Dopenapi.update=true`: 50 ITs, 0 failures, BUILD SUCCESS |

Outside `local`, the cookie carries `Secure` (shown at the end of the no-profile output).

One deliberate deviation from the brief: under a non-local profile an **anonymous** `dev-sign-in` request is
401, not 404, because the security chain permits that path only when the `local` profile is active.
Permitting it everywhere would make the absent route answer 404 to anonymous callers and so stand out from
every other unknown path. With a valid token it is 404, which proves the handler is absent.

## Output — `local` profile (port 8380)

```text
## TC-01 dev sign-in, web mode, new user
$ POST /dev-sign-in (web)
HTTP/1.1 200 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Access-Control-Allow-Origin: http://localhost:8081
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Allow-Credentials: true
Set-Cookie: rikkaus_refresh=<redacted>; Path=/api/v1/auth; Max-Age=2592000; Expires=Tue, 22 Dec 2026 09:43:47 GMT; HttpOnly; SameSite=Strict

{"accessToken":"<redacted>","tokenType":"Bearer","expiresInSeconds":900,"user":{"id":"83374070-ba63-452f-8131-f2d782d00eae","email":"an.nguyen@example.com","displayName":"An Nguyễn"}}

cookie=<len:43 sha:d2e18141> body.refreshToken present: false
users row: local-dev|an.nguyen@example.com|an.nguyen@example.com|An Nguyễn

## TC-02 dev sign-in again, same email
status HTTP/1.1 200 ; same user id: yes; rows: 1

## TC-03 access token works
$ GET /session (bearer from TC-01)
HTTP/1.1 200 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers

{"id":"83374070-ba63-452f-8131-f2d782d00eae","email":"an.nguyen@example.com","displayName":"An Nguyễn"}

## TC-04 rotation via cookie
$ POST /refresh (web, cookie, no body)
HTTP/1.1 200 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Access-Control-Allow-Origin: http://localhost:8081
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Allow-Credentials: true
Set-Cookie: rikkaus_refresh=<redacted>; Path=/api/v1/auth; Max-Age=2592000; Expires=Tue, 22 Dec 2026 09:43:47 GMT; HttpOnly; SameSite=Strict

{"accessToken":"<redacted>","tokenType":"Bearer","expiresInSeconds":900,"user":{"id":"83374070-ba63-452f-8131-f2d782d00eae","email":"an.nguyen@example.com","displayName":"An Nguyễn"}}

old cookie=<len:43 sha:d2e18141> new cookie=<len:43 sha:fa221e87> differs: yes; body.refreshToken present: false

## TC-05 reuse of rotated cookie revokes the session
$ POST /refresh with OLD cookie
HTTP/1.1 401 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Access-Control-Allow-Origin: http://localhost:8081
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Allow-Credentials: true

{"detail":"Authentication required.","instance":"/api/v1/auth/refresh","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized","correlationId":"7821d27c-75f8-4f79-bc0b-eda5f3ef6159"}

$ POST /refresh with the newer cookie after reuse
HTTP/1.1 401 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Access-Control-Allow-Origin: http://localhost:8081
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Allow-Credentials: true

{"detail":"Authentication required.","instance":"/api/v1/auth/refresh","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized","correlationId":"32e6a4c8-90c1-4b55-ac0e-6d7bedbb3927"}

chain of first session: 2 tokens, 2 revoked

## TC-06 logout via cookie clears the cookie
$ POST /logout (web, cookie)
HTTP/1.1 204 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Access-Control-Allow-Origin: http://localhost:8081
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Allow-Credentials: true
Set-Cookie: rikkaus_refresh=; Path=/api/v1/auth; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; HttpOnly; SameSite=Strict



$ POST /refresh with the logged-out cookie
HTTP/1.1 401 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Access-Control-Allow-Origin: http://localhost:8081
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Allow-Credentials: true

{"detail":"Authentication required.","instance":"/api/v1/auth/refresh","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized","correlationId":"3d7e792b-7591-46a0-a46b-9d187ffd8dae"}

## TC-07 CSRF: cookie without web header
$ POST /refresh (cookie only, no X-Rikkaus-Client)
HTTP/1.1 403 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers

{"detail":"Access denied.","instance":"/api/v1/auth/refresh","status":403,"title":"Access denied","type":"urn:rikkaus:problem:forbidden","correlationId":"6ab72979-6c3d-4340-b9e6-0ab0fe41df76"}

follow-up proper web refresh with same cookie: HTTP/1.1 200  (token was not consumed)

## TC-08 CSRF: foreign Origin
$ POST /refresh (web header, Origin https://evil.example)
HTTP/1.1 403 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers

Invalid CORS request

follow-up proper web refresh with same cookie: HTTP/1.1 200 

## TC-09 logout CSRF: cookie without header
$ POST /logout (cookie only)
HTTP/1.1 403 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers

{"detail":"Access denied.","instance":"/api/v1/auth/logout","status":403,"title":"Access denied","type":"urn:rikkaus:problem:forbidden","correlationId":"3f0ffdcd-9a36-429a-add9-03469ca66d0e"}

session still renewable: HTTP/1.1 200 

## TC-10 neither body token nor cookie
$ POST /refresh {} (web, no cookie)
HTTP/1.1 400 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Access-Control-Allow-Origin: http://localhost:8081
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Allow-Credentials: true

{"detail":"Request validation failed.","instance":"/api/v1/auth/refresh","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be blank","field":"refreshToken"}],"correlationId":"f0197cb9-7770-4850-b147-eef6195b2c7b"}

$ POST /logout {} (native)
HTTP/1.1 400 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers

{"detail":"Request validation failed.","instance":"/api/v1/auth/logout","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be blank","field":"refreshToken"}],"correlationId":"8b4dcdce-4ecc-49ff-9262-fa476d94419a"}

## TC-11 CORS preflight, allowed origin, auth route
$ OPTIONS /auth/refresh (Origin http://localhost:8081)
HTTP/1.1 200 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Access-Control-Allow-Origin: http://localhost:8081
Access-Control-Allow-Methods: GET,POST
Access-Control-Allow-Headers: content-type, x-rikkaus-client
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 1800



## TC-12 CORS preflight, unknown origin
$ OPTIONS /auth/refresh (Origin https://evil.example)
HTTP/1.1 403 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers

Invalid CORS request

## TC-13 CORS on a non-auth route stays credential-free
$ OPTIONS /api/v1/assets (Origin http://localhost:8081)
HTTP/1.1 200 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Access-Control-Allow-Origin: http://localhost:8081
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE
Access-Control-Allow-Headers: authorization
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Max-Age: 1800



## TC-14 native body flow unchanged
$ POST /dev-sign-in (native)
HTTP/1.1 200 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers

{"accessToken":"<redacted>","tokenType":"Bearer","expiresInSeconds":900,"refreshToken":"<redacted>","user":{"id":"ada1d418-1703-497e-92e7-e744aeffac9a","email":"binh.tran@example.com","displayName":"Bình Trần"}}

$ POST /refresh (native, body token)
HTTP/1.1 200 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers

{"accessToken":"<redacted>","tokenType":"Bearer","expiresInSeconds":900,"refreshToken":"<redacted>","user":{"id":"ada1d418-1703-497e-92e7-e744aeffac9a","email":"binh.tran@example.com","displayName":"Bình Trần"}}

rotated body token: <len:43 sha:aacbb7f6> -> <len:43 sha:096c3247>
$ POST /logout (native, body token)
HTTP/1.1 204 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Set-Cookie: rikkaus_refresh=; Path=/api/v1/auth; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; HttpOnly; SameSite=Strict



## TC-15 native reuse still revokes
$ POST /refresh reusing the first body token
HTTP/1.1 401 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers

{"detail":"Authentication required.","instance":"/api/v1/auth/refresh","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized","correlationId":"66a8005f-0419-474a-a58a-f17a4dfef7e9"}

$ POST /refresh with the successor after reuse
HTTP/1.1 401 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers

{"detail":"Authentication required.","instance":"/api/v1/auth/refresh","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized","correlationId":"4e1c2674-58e4-485d-89f4-1d04f3aae741"}

## TC-16 dev sign-in validation
$ POST /dev-sign-in {email: not-an-email}
HTTP/1.1 400 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers

{"detail":"Invalid request content.","instance":"/api/v1/auth/dev-sign-in","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must be an email address","field":"email"}],"correlationId":"bc575142-f778-4636-962a-29e909aafcda"}

## TC-19 Google sign-in route still validates
$ POST /google {} (web)
HTTP/1.1 400 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Access-Control-Allow-Origin: http://localhost:8081
Access-Control-Expose-Headers: X-Correlation-Id
Access-Control-Allow-Credentials: true

{"detail":"Invalid request content.","instance":"/api/v1/auth/google","status":400,"title":"Request validation failed","type":"urn:rikkaus:problem:validation-failed","errors":[{"message":"must not be blank","field":"redirectUri"},{"message":"must not be blank","field":"authorizationCode"},{"message":"must not be blank","field":"codeVerifier"}],"correlationId":"4297981a-de8e-4192-9bdd-6a721af9aa78"}

```

## Output — no profile (port 8381)

```text
## TC-17 dev sign-in absent outside the local profile (same jar, no profile, port 8381)
"message":"No active profile set, falling back to 1 default profile: \"
$ POST :8381/api/v1/auth/dev-sign-in (no token)
HTTP/1.1 401 
Content-Type: application/problem+json
Content-Length: 181

{"detail":"Authentication required.","status":401,"title":"Authentication required","type":"urn:rikkaus:problem:unauthorized","correlationId":"95c91dbf-4daf-4d74-83ca-19847a104bb9"}

$ POST :8381/api/v1/auth/dev-sign-in (valid bearer, same signing secret)
HTTP/1.1 404 
Content-Type: application/problem+json

{"detail":"Resource not found.","instance":"/api/v1/auth/dev-sign-in","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found","correlationId":"4921d4a9-eef8-4f96-b7b8-47f0e4e2b690"}

$ GET :8381/api/v1/auth/session with the same bearer (control: token is accepted here)
200

## TC-08b origin check without any CORS mapping (no-profile run has an empty allowlist)
$ POST :8381/api/v1/auth/refresh (X-Rikkaus-Client: web, Origin: https://evil.example, cookie)
HTTP/1.1 403 
Content-Type: application/problem+json

{"detail":"Access denied.","instance":"/api/v1/auth/refresh","status":403,"title":"Access denied","type":"urn:rikkaus:problem:forbidden","correlationId":"872c17ae-170c-41e0-aeba-99764ddf7269"}

follow-up web refresh on :8380 with the same cookie: 200 (not consumed by the refused request)

## Secure attribute outside local (no-profile run)
Set-Cookie: rikkaus_refresh=<redacted>; Path=/api/v1/auth; Max-Age=2592000; Expires=Tue, 22 Dec 2026 09:44:10 GMT; Secure; HttpOnly; SameSite=Strict
```
