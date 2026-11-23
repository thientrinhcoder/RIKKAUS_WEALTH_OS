# Manual test guide — sessions, the web refresh cookie, and local dev sign-in

The web refresh cookie and the `local`-only development sign-in (issue #41) ship with no new automated
test, by Product Owner direction. This guide is the replacement and also the way to obtain a real session
on a machine without a Google OAuth client. The contract is in
[`api-contract-conventions.md`](api-contract-conventions.md#web-session-cookie); the full case list is
[`plans/261122-1629-GH-41-be-auth-contract/test-cases.md`](../plans/261122-1629-GH-41-be-auth-contract/test-cases.md);
the recorded run is
[`plans/reports/manual-verification-261122-1638-GH-41-be-auth-contract.md`](../plans/reports/manual-verification-261122-1638-GH-41-be-auth-contract.md).

Nothing here needs a Google account.

## 1. Start Postgres and the API

A shell in the repository root, Docker, and JDK 25 (the system default is often 21). Reuse a running
`rikkaus_wealth_os-postgres-1` container if `docker ps` shows one.

```bash
docker compose up -d postgres
```

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./services/api/mvnw -f services/api/pom.xml package -DskipTests
```

Find the jar with `find services/api -name 'rikkaus-wealth-api-*.jar'` and run it with the **`local`**
profile — the development sign-in does not exist under any other:

```bash
SERVER_PORT=8380 /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home/bin/java -jar <jar> --spring.profiles.active=local
```

The `local` profile allows the origin `http://localhost:8081` (Expo web) and sets
`rikkaus.auth.refresh-cookie.secure=false` because it serves plain http.

## 2. Get a session (agents and developers)

### Native style — token in the body

```bash
curl -s -X POST http://localhost:8380/api/v1/auth/dev-sign-in -H 'Content-Type: application/json' -d '{"email":"an.nguyen@example.com","displayName":"An Nguyễn"}'
```

Answer 200, the same shape as `/auth/google`:
`{"accessToken":"…","tokenType":"Bearer","expiresInSeconds":900,"refreshToken":"…","user":{"id":"…","email":"an.nguyen@example.com","displayName":"An Nguyễn"}}`.
The account is created on first use (`google_subject = local-dev|an.nguyen@example.com`) and reused
afterwards. Use `accessToken` as `Authorization: Bearer …` on any domain route.

### Web style — token in the cookie

```bash
curl -s -c jar.txt -X POST http://localhost:8380/api/v1/auth/dev-sign-in -H 'Content-Type: application/json' -H 'X-Rikkaus-Client: web' -H 'Origin: http://localhost:8081' -d '{"email":"an.nguyen@example.com","displayName":"An Nguyễn"}'
```

The body has no `refreshToken`; the response carries
`Set-Cookie: rikkaus_refresh=…; Path=/api/v1/auth; Max-Age=2592000; HttpOnly; SameSite=Strict`.

From the web app (Expo web on `http://localhost:8081`), the same call is:

```js
await fetch("http://localhost:8380/api/v1/auth/dev-sign-in", {
  method: "POST",
  credentials: "include",
  headers: { "Content-Type": "application/json", "X-Rikkaus-Client": "web" },
  body: JSON.stringify({
    email: "an.nguyen@example.com",
    displayName: "An Nguyễn",
  }),
});
```

### From the Expo web app

Start Expo web with the development sign-in enabled (dev bundles only; `expo export` never includes it):

```bash
EXPO_PUBLIC_API_BASE_URL=http://localhost:8380 EXPO_PUBLIC_DEV_SIGN_IN=local npx expo start --web
```

`/sign-in` then shows **Đăng nhập phát triển** below the Google entry. It calls the same route in web
mode, so the browser holds `rikkaus_refresh` and a reload restores the session. Expo web on a port other
than 8081 needs that origin in `API_ALLOWED_ORIGINS`. The executed frontend cases are in
[`plans/261123-GH-41-identity-session/test-cases.md`](../plans/261123-GH-41-identity-session/test-cases.md).

## 3. Check the cookie lifecycle

| Step                        | Command                                                                                                                                                                                                          | Expect                                                                     |
| --------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| Renew via cookie            | `curl -s -b jar.txt -c jar.txt -X POST http://localhost:8380/api/v1/auth/refresh -H 'X-Rikkaus-Client: web' -H 'Origin: http://localhost:8081'`                                                                  | 200, new `accessToken`, no `refreshToken` in body, new cookie in `jar.txt` |
| CSRF: cookie without header | `curl -s -b jar.txt -X POST http://localhost:8380/api/v1/auth/refresh`                                                                                                                                           | 403 `urn:rikkaus:problem:forbidden`; the cookie still works afterwards     |
| CSRF: foreign origin        | `curl -s -b jar.txt -X POST http://localhost:8380/api/v1/auth/refresh -H 'X-Rikkaus-Client: web' -H 'Origin: https://evil.example'`                                                                              | 403 `Invalid CORS request`; the cookie still works afterwards              |
| Reuse revokes               | save the cookie, renew once, then renew with the saved value                                                                                                                                                     | 401 `unauthorized`, and the newer cookie is 401 too                        |
| Sign out                    | `curl -s -i -b jar.txt -c jar.txt -X POST http://localhost:8380/api/v1/auth/logout -H 'X-Rikkaus-Client: web' -H 'Origin: http://localhost:8081'`                                                                | 204 with `Set-Cookie: rikkaus_refresh=; Max-Age=0`                         |
| Preflight                   | `curl -s -i -X OPTIONS http://localhost:8380/api/v1/auth/refresh -H 'Origin: http://localhost:8081' -H 'Access-Control-Request-Method: POST' -H 'Access-Control-Request-Headers: content-type,x-rikkaus-client'` | 200 with `Access-Control-Allow-Credentials: true` and the exact origin     |

## 4. Confirm the development sign-in is absent elsewhere

Run the same jar without `--spring.profiles.active=local` (supply `RIKKAUS_JWT_SECRET`, 32+ bytes) and
`POST /api/v1/auth/dev-sign-in`: 401 without a token, 404 `not-found` with a valid one. It is also absent
from `services/api/openapi/openapi.json`.

## 5. Clean up

Stop the API process you started. Development accounts can be removed with
`delete from wealth.users where google_subject like 'local-dev|%';` (their sessions cascade).
