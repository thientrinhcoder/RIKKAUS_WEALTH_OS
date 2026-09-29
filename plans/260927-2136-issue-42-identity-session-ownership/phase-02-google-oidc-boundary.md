---
phase: 2
title: "Google OIDC boundary and first login"
status: completed
priority: P1
effort: "10h"
dependencies: [1]
---

# Phase 2: Google OIDC boundary and first login

## Objective

Turn a Google authorization code into a Rikkaus session. First login creates the account; a later
login resumes it. The whole path is testable offline because Google sits behind two ports.

## Context

The #40 state matrix defines the flow the backend must complete: the app starts the handoff, Google
returns, and the app shows the account outcome "without exposing tokens, raw claims, or provider
internals". The #40 non-goals hand the token exchange, PKCE, nonce and storage design to this task.

The user chose backend-side code exchange with PKCE, so the client secret never reaches the client and
the browser never holds a Google token.

## Requirements

### Ports, so tests never call Google

```java
interface GoogleAuthorizationCodeExchange {   // code + verifier -> raw ID token
    String exchange(String authorizationCode, String codeVerifier, String redirectUri);
}

interface GoogleIdentityTokenVerifier {       // raw ID token -> verified claims
    GoogleIdentity verify(String idToken);
}

record GoogleIdentity(String subject, String email, boolean emailVerified, String displayName) {}
```

Adapters:

- `HttpGoogleAuthorizationCodeExchange` posts to Google's token endpoint with `RestClient`. Any
  non-2xx, or a body without `id_token`, becomes `unauthorized` — never a 500, because a rejected or
  expired code is a normal user outcome (the "callback failure" state), not a server fault.
- `NimbusGoogleIdentityTokenVerifier` wraps a `JwtDecoder` built from Google's JWKS URI with
  validators for issuer (`https://accounts.google.com` or `accounts.google.com`, both of which Google
  issues), audience (our client id) and expiry. Rejecting an unverified `email_verified` claim is
  required: an unverified address must not become an account identity.

### `redirect_uri` allowlist

`rikkaus.auth.google.redirect-uris` is a comma-separated allowlist, parsed the way
`CorsConfiguration` parses its origins, and **empty permits nothing**. A client-supplied
`redirectUri` outside the list is `validation-failed` (400) and is never forwarded to Google.
Forwarding an arbitrary redirect URI is how authorization codes get delivered to an attacker.

### `IdentityService.signInWithGoogle`

1. Exchange the code, verify the identity token.
2. Reject `emailVerified == false` with `unauthorized`.
3. Find by `google_subject`. Absent → create the account (this is first-login account creation,
   exactly as the accepted design describes it: an app-side result after Google returns an approved
   claim). Present → refresh `email`, `display_name` and `updated_at`.
4. Issue an access token and a refresh token in a new `session_id`.

### HTTP surface

`POST /api/v1/auth/google`, in `com.rikkaus.wealth.identity.api`, so
`ArchitectureRulesTest.controllersLiveInAnApiPackage` is satisfied.

Request, all fields `@NotBlank`:

```json
{ "authorizationCode": "4/0Ab...", "codeVerifier": "dBjftJeZ4CVP...", "redirectUri": "http://localhost:8081/auth/callback" }
```

Response `200`:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "refreshToken": "9qL3x...",
  "user": { "id": "0f2c...", "email": "an.nguyen@example.com", "displayName": "An Nguyễn" }
}
```

Documented statuses: 200, 400 `validation-failed`, 401 `unauthorized`, 405, 415. Not 403 — there is
no authenticated-but-forbidden outcome on a sign-in route.

## Files

| Action | Path |
|---|---|
| Create | `.../identity/google/GoogleAuthorizationCodeExchange.java` |
| Create | `.../identity/google/GoogleIdentityTokenVerifier.java` |
| Create | `.../identity/google/GoogleIdentity.java` |
| Create | `.../identity/google/HttpGoogleAuthorizationCodeExchange.java` |
| Create | `.../identity/google/NimbusGoogleIdentityTokenVerifier.java` |
| Create | `.../identity/google/GoogleOidcProperties.java` |
| Create | `.../identity/application/IdentityService.java` |
| Create | `.../identity/api/AuthenticationController.java` |
| Create | `.../identity/api/GoogleSignInRequest.java` |
| Create | `.../identity/api/SessionResponse.java` |
| Create | `.../identity/api/AuthenticatedUserResponse.java` |
| Modify | `services/api/src/main/resources/application.yml`, `application-local.yml`, `.env.example` |
| Create | `.../identity/application/IdentityServiceTest.java` |
| Create | `.../identity/google/GoogleOidcPropertiesTest.java` |
| Create | `.../identity/api/AuthenticationControllerTest.java` |

## Steps

1. **3.T** — confirm Phase 1's suite is green before touching anything.
2. Write `IdentityServiceTest` with stubbed ports for: first login creates, repeat login resumes and
   refreshes the profile, unverified email rejected, disallowed redirect URI rejected.
3. Implement the ports, properties and service until green.
4. Write `AuthenticationControllerTest` as a `@WebMvcTest` slice for the happy path and the two 400
   shapes, then implement the controller.
5. **3.V** — `mvnw test` green, no pre-existing test regressed.

## Validation

- Unit and slice tests green.
- No test reaches the network; assert this by running with the network-dependent adapter replaced by a
  stub in every test that exercises the service.
- Confirm no log statement carries the authorization code, the Google ID token or the refresh token.

## Risk

| Risk | Mitigation |
|---|---|
| Open redirect via a client-supplied `redirectUri`. | Allowlist, empty-permits-nothing, validated before the exchange call. |
| Unverified Google email becomes an account identity. | Explicit `emailVerified` gate with a unit test. |
| A rejected code returns 500 and reads as a server outage in the UI. | Adapter maps every non-2xx to `unauthorized`; asserted. |

## Rollback

Delete the `identity/google`, `identity/application` and `identity/api` packages. Phase 1 stands
alone; no migration changes here.
