# GH-41 Frontend — identity, session and ownership boundary: test cases

Written before implementation. No unit tests by project rule; every case below is executed by hand
(scripted browser) against the real API jar and the shared compose Postgres, with screenshots at
360px and 1440px. Results: `plans/reports/manual-verification-261123-GH-41-identity-session.md`.

## Setup shared by every case

- Postgres `rikkaus_wealth_os-postgres-1` (127.0.0.1:5432), reused.
- API jar on `:8580`, profile `local`, `API_ALLOWED_ORIGINS=http://localhost:8095`, and for TC-04 only
  `rikkaus.auth.jwt.access-token-ttl=PT1M`.
- Expo web on `:8095` with `EXPO_PUBLIC_API_BASE_URL=http://localhost:8580`,
  `EXPO_PUBLIC_DEV_SIGN_IN=local`, and a placeholder `EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID` (no real Google
  client exists locally, so the Google flow is verified up to the redirect).
- Users: `an.nguyen@example.com` / `An Nguyễn` (returning, has a USD/VND rate and an asset) and a fresh
  `gh41.moi.<run>@example.com` (first sign-in). Owner B `binh.tran@example.com` owns one asset, created
  with curl.
- Storage audit (TC-11) runs after every step of every case: `localStorage.length === 0`,
  `sessionStorage.length === 0`, `indexedDB.databases()` empty, and no token text in `document.cookie`.

## TC-01 First sign-in

- **Given** a fresh email with no Rikkaus account and no exchange rate, on `/sign-in`
  showing `Tài sản của bạn. Một góc nhìn riêng tư.` and the CTA `Tiếp tục với Google`
- **When** the developer signs in through the local section `Đăng nhập phát triển`
- **Then** `POST /api/v1/auth/dev-sign-in` carries `X-Rikkaus-Client: web` and is sent with credentials;
  the 200 response sets `rikkaus_refresh` (HttpOnly, SameSite=Strict, Path=/api/v1/auth, no Secure under
  `local`) and its body has no `refreshToken`
- **And** the screen shows `Tài khoản Rikkaus đã sẵn sàng` with `Thiết lập tỷ giá USD/VND`.

## TC-02 Returning sign-in

- **Given** `an.nguyen@example.com` already has an account and a USD/VND rate
- **When** they sign in from `/sign-in`
- **Then** the screen shows `Đã đăng nhập` / `Phiên Rikkaus đã được khôi phục…` with `Mở Tổng quan`
- **And** `Mở Tổng quan` opens the shell (`Tổng quan`, five destinations) and the profile screen shows
  `An Nguyễn` and the email.

## TC-03 Reload keeps the session via the cookie

- **Given** a signed-in web session on `/`
- **When** the page is reloaded
- **Then** the first API call is `POST /api/v1/auth/refresh` with `X-Rikkaus-Client: web`, body `{}`,
  the cookie attached by the browser; it answers 200 with a rotated `rikkaus_refresh`
- **And** the shell renders without passing through `/sign-in`; no token exists in any web storage.

## TC-04 Access-token expiry triggers exactly one refresh for concurrent requests

- **Given** the API runs with a 1-minute access token, and the user is on an asset detail screen
  (`/holdings/<id>`), which keeps two queries live: the asset and `GET /api/v1/auth/session`
- **When** more than 60 seconds pass and the tab regains focus, so both queries refetch at once
- **Then** both requests answer 401 `unauthorized`, exactly **one** `POST /api/v1/auth/refresh` is
  sent, and both requests are resent once with the new token and succeed
- **And** the screen still shows the asset; no session-ended screen appears.

## TC-05 Refresh-token reuse → session ended (revoked) screen

- **Given** a signed-in session and a saved copy of the current `rikkaus_refresh` value
- **When** the app rotates the cookie (reload), then the old value is put back into the browser and
  the access token has expired, so the next refresh presents the retired value
- **Then** the server revokes the session (401) and the app **replaces** the screen content with
  `Phiên đăng nhập đã kết thúc` / `Vì lý do bảo mật, hãy đăng nhập lại…`, actions `Đăng nhập lại` and
  `Về màn hình đầu`; no financial value is in the DOM behind it
- **And** after signing in again the app opens `Tổng quan`, not the previous deep link.

## TC-06 Expired session on reload

- **Given** a browser holding a `rikkaus_refresh` cookie the server no longer accepts (revoked)
- **When** the app loads `/`
- **Then** `/auth/refresh` answers 401 and the app shows `Phiên đăng nhập đã hết hạn` /
  `Để bảo vệ dữ liệu tài chính, nội dung trước đó đã được ẩn…` with `Tiếp tục với Google` and
  `Về màn hình đầu`.

## TC-07 Sign-out clears the cookie and redirects

- **Given** a signed-in session, on the profile screen (`Hồ sơ`)
- **When** the user taps `Đăng xuất` and confirms `Đăng xuất khỏi Rikkaus?` → `Đăng xuất`
- **Then** `POST /api/v1/auth/logout` (web header, credentials, body `{}`) answers 204 with
  `Set-Cookie: rikkaus_refresh=; Max-Age=0`, the browser no longer holds the cookie, and the app shows
  `/sign-in`
- **And** reloading stays on `/sign-in` (refresh answers 400 because no cookie is sent), and the
  browser Back button does not show any signed-in content.
- **And** cancelling with `Tiếp tục sử dụng` keeps the session untouched.

## TC-08 Signed-out deep link is redirected

- **Given** no session
- **When** the user opens `/holdings/<own asset id>` or `/` directly
- **Then** the app redirects to `/sign-in`; after signing in it opens `Tổng quan`.

## TC-09 Cross-user asset id → not-found

- **Given** user A signed in, and an asset id owned by user B
- **When** A opens `/holdings/<B's id>`
- **Then** `GET /api/v1/holdings/<id>` answers 404 `not-found` and the screen shows
  `Không thể mở mục này` / `Nội dung không khả dụng. Không có thông tin nào về bản ghi hoặc chủ sở hữu
được hiển thị.` with `Quay lại Tổng quan`
- **And** a random non-existent UUID shows the identical screen (no enumeration); A's own asset id shows
  its name and value.

## TC-10 Google sign-in: redirect, cancelled, denied, failed

- **Given** `/sign-in`, Google client id configured
- **When** the user taps `Tiếp tục với Google`
- **Then** a Google window opens at `https://accounts.google.com/o/oauth2/v2/auth` with
  `response_type=code`, `code_challenge_method=S256`, a `code_challenge`, `scope=openid email profile`
  and `redirect_uri=http://localhost:8095/auth/callback` (verified up to the redirect only)
- **When** the user closes the Google window → `Bạn đã dừng đăng nhập` with `Thử lại với Google` and
  `Về màn hình đầu`
- **When** Google redirects back with `error=access_denied` → `Chưa thể tiếp tục` with
  `Xem lại với Google` and `Quay lại`
- **When** Google redirects back with a code the API rejects → `Không hoàn tất được đăng nhập` /
  `Kết quả trả về chưa được xử lý. Không có thay đổi nào được lưu.` with `Thử lại`.

## TC-11 Offline / network error

- **Given** the API is unreachable
- **When** the app loads `/`
- **Then** it shows `Không kết nối được máy chủ` with `Thử lại` (not the sign-in screen, and no
  fabricated content); `Thử lại` after the API is back restores the session
- **When** the developer signs in while the API is unreachable → the sign-in screen shows the same
  network error and no session is created.

## TC-12 Storage audit

- **Given** any step of TC-01…TC-11
- **Then** `localStorage` and `sessionStorage` are empty, IndexedDB has no database, and
  `document.cookie` does not expose `rikkaus_refresh` (HttpOnly).

## TC-13 Production build hides development sign-in

- **Given** `npm run build:web` without `EXPO_PUBLIC_DEV_SIGN_IN`
- **Then** the exported bundle contains no `dev-sign-in` path and `/sign-in` shows only
  `Tiếp tục với Google`.
