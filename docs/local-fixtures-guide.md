# Local fixtures guide

Three scripts in [`services/api/fixtures`](../services/api/fixtures) put realistic data behind the local
API so the frontend can review real screens at phone and desktop widths, check keyboard and screen-reader
behaviour on long lists, and reproduce the design guideline's baseline (section 16.3 of
[`design-guidelines.md`](design-guidelines.md)) without typing it in. Issue #107.

Every record goes through the public API — the same validation, ownership and rounding a client's request
gets. The only route the scripts use that a client does not is the development sign-in, which exists only
under the `local` Spring profile ([`api-contract-conventions.md`](api-contract-conventions.md#local-development-sign-in)).

## What you need

- The API running with `--spring.profiles.active=local` (see
  [`manual-test-guide-auth.md`](manual-test-guide-auth.md#1-start-postgres-and-the-api)).
- `bash`, `curl` 7.55 or later, and `jq`. macOS and Linux `date` both work.
- `API_BASE_URL`, the API's origin. The default is `http://localhost:8107`.

The scripts never print the access token. They keep it in a private temporary file that is deleted when
the script ends, successful or not, and never pass it on a command line. They contain no secret.

## The scripts

| Script | Default account | What it does |
|---|---|---|
| `reset.sh [email]` | `an.nguyen@example.com` | Deletes every asset, liability, cash-flow entry and goal of the account (linked ones with `confirm=true`) |
| `load-baseline.sh [email]` | `an.nguyen@example.com` | Resets, then loads the design baseline |
| `load-long-lists.sh [email]` | `long.list@example.com` | Resets, then loads long lists |

Each load script starts with a reset, so running it twice leaves the same records (with new ids). The API
has no route that deletes the declared exchange rate, onboarding progress or feedback; those survive a
reset. For an account that has never declared a rate — the dashboard's partial state — sign in with an
email no script has used.

```bash
API_BASE_URL=http://localhost:8107 ./services/api/fixtures/load-baseline.sh
```

```bash
API_BASE_URL=http://localhost:8107 ./services/api/fixtures/load-long-lists.sh
```

```bash
API_BASE_URL=http://localhost:8107 ./services/api/fixtures/reset.sh long.list@example.com
```

A request the API refuses stops the script and prints the problem document, so a fixture that no longer
matches the contract fails loudly instead of loading half a dataset.

### The baseline

`load-baseline.sh` loads exactly the guideline's figures. Dates that must stay recent, stale or upcoming
are computed from today in `Asia/Ho_Chi_Minh`, so the fixture holds whatever day it runs.

| Record | Value |
|---|---|
| Exchange rate | 1 USD = 25.100 VND, effective 12/09/2026 |
| `Tài khoản thanh toán Vietcombank` | 650.000.000 ₫, valued 2 days ago |
| `Tiền gửi kỳ hạn 12 tháng` | 1.200.000.000 ₫ |
| `Danh mục cổ phiếu HOSE` | 2.150.000.000 ₫ |
| `Căn hộ Thảo Điền` | 7.500.000.000 ₫, **valued 75 days ago** (the stale valuation; 62,5 % of assets, the concentration) |
| `Tiền gửi USD` | 20.000 USD = 502.000.000 ₫ |
| `Vay mua nhà Vietcombank` | 1.500.000.000 ₫, 18.750.000 ₫ monthly, **next payment in 12 days**, secured by the apartment |
| `Thẻ tín dụng Techcombank Visa` | 32.000.000 ₫, next payment in 40 days (outside the 30-day window) |
| Monthly income | `Lương và thưởng hiệu suất hằng tháng` 180.000.000 ₫ |
| Monthly expenses | 52.250.000 + 21.000.000 + 18.750.000 (linked to the mortgage) = 92.000.000 ₫ |
| `Quỹ du học cho con` | **`AT_RISK`**: the contribution is set to 90 % of what the projection requires today |
| `Quỹ dự phòng 6 tháng chi tiêu` | `ON_TRACK`: 130 % of the required contribution |
| Onboarding | `COMPLETED`, so the app opens on the dashboard |

`GET /api/v1/wealth-snapshot` then reads total assets `12002000000.00`, total liabilities
`1532000000.00`, net worth `10470000000.00`, and this month's report reads income `180000000.00`,
expenses `92000000.00`, net `88000000.00`. Deleting `Căn hộ Thảo Điền` or `Vay mua nhà Vietcombank`
without `confirm=true` returns `409 linked-records`, which is how the confirmation dialog can be
reviewed.

### The long lists

`load-long-lists.sh` loads 60 assets, 24 valuation records on one of them, 40 liabilities, 120 cash-flow
entries, 15 goals and 20 feedback entries in about 6 seconds. It is built to stress layout rather than to
reconcile: every category and currency, long Vietnamese names with diacritics (one of exactly 120
characters, the maximum), `Đất nền Long An` beside `Dat nen Long An khong dau`, two pairs of identical
names on the same date, overdue, upcoming, later and missing payment dates, entries with and without
notes, and USD amounts. Feedback has no delete route, so only the entries missing from 20 are added.

## Using it from the app

The app's development sign-in defaults to `an.nguyen@example.com`, the baseline account. Point Expo at
the same API and sign in with **Đăng nhập phát triển**; type `long.list@example.com` there to see the long
lists.

```bash
EXPO_PUBLIC_API_BASE_URL=http://localhost:8107 EXPO_PUBLIC_DEV_SIGN_IN=local npx expo start --web
```

The `local` profile allows the origin `http://localhost:8081`, where Expo web serves. Another port needs
that origin in `API_ALLOWED_ORIGINS` when the API starts.

What the lists promise, so no screen needs a layout-specific workaround, is in
[Pilot list bounds, error shape and local fixtures](api-contract-conventions.md#pilot-list-bounds-error-shape-and-local-fixtures--implemented).
