# Manual verification — GH-44 money, currency, exchange-rate & time primitives

Every case in [`test-cases.md`](../261122-1629-GH-44-money-time-primitives/test-cases.md) was executed
on 22/11/2026 (Asia/Ho_Chi_Minh). All of them pass. No unit tests were added (Product Owner
direction); the evidence below is console output from the real modules, the real API, and the
rendered catalog. Files are in [`gh-44-evidence/`](gh-44-evidence/).

## Result

| Group | Cases | Route | Result | Evidence |
|---|---|---|---|---|
| 1. Decimal intake | D1–D10 | node | 10/10 | `formatter-cases-tz-*.log` |
| 2. Money display | M1–M12 | node + catalog | 11/11 node (M5 is UI-only), M1/M3/M4/M5/M6/M7 in the DOM with matching `aria-label` | logs, `catalog-money-360-1-money.jpg`, `catalog-money-1440.png` |
| 3. Input parsing | P1–P14 | node + catalog | 14/14; P5 and P9 typed into the catalog show the inline error | logs, `parse-errors-360.jpg` |
| 4. Rate & conversion | R1–R10 | node + live API | 8/8 node; R9/R10 against the real API | logs, `live-api-cases.log`, `live-exchange-rate-response.txt` |
| 5. Percentages | Q1–Q6 | node + catalog | 5/5 node; Q5 renders `— Không khả dụng` + reason | logs, `catalog-money-360-2-percent-time.jpg` |
| 6. Dates & time | T1–T10 | node in 3 zones + catalog | 9/9 identical under `Asia/Ho_Chi_Minh`, `UTC` and `America/Los_Angeles`; T10 disclosure in the DOM | logs, `catalog-money-360-2-percent-time.jpg` |
| 7. Freshness | S1–S6 | node + catalog + live | 6/6; S6 live: declared `2026-09-12`, today `2026-11-22` → stale, 71 days | logs, `catalog-money-360-3-freshness-unavailable.jpg`, `live-api-360.jpg` |
| 8. Unavailable | U1–U2 | catalog | Pass; reason wraps at 360px, `scrollWidth` 360 = viewport | `catalog-money-360-3-freshness-unavailable.jpg` |
| 9. Live assets | L1–L2 | live API | Pass (below) | `live-api-cases.log`, `live-api-360.jpg`, `live-api-1440.jpg` |
| 10. Regression | G1–G3 | catalog + CLI | Pass | `catalog-forms-360-regression.jpg`, `catalog-data-1440.png` |

Node runner totals: `63 passed, 0 failed` in each of the three time zones.

## Live API (R9, R10, S6, T6, L1, L2)

API built from this branch, `local` profile, port 8280, against the root compose Postgres. A
dedicated user `44440000-0000-4000-8000-000000000044` was inserted so no sibling session's data was
touched; its token was minted per `docs/manual-test-guide-assets.md` §4 and never printed.

- `GET /settings/exchange-rate` before any declaration: `200`, all three rate fields `null` →
  `readExchangeRateSettings` returns `{ rate: null }`, rendered as the rate-needed state.
- `PUT {"usdToVndRate": 25100.000000, "effectiveDate": "2026-09-12"}`: `200`. Rendered:
  `1 USD = 25.100 VND · hiệu lực 12/09/2026`, `Cập nhật lúc 16:47, 22/11/2026 (GMT+7)`,
  UTC source `2026-11-22T09:47:02.967Z`.
- Three assets created through `POST /assets`, read through `GET /assets`. All `currentValue`
  members arrived as JSON numbers; `precisionAtRisk` was `false` for each.

| Asset | JSON | Rendered |
|---|---|---|
| Căn hộ Riverside | `12002000000.00` VND | `12.002.000.000 ₫` |
| Tài khoản USD | `20000.00` USD | `502.000.000 ₫`, `20.000 USD`, `Tỷ giá 25.100 VND/USD · hiệu lực 12/09/2026` |
| Tiền gửi USD lẻ | `1250.50` USD | `31.387.550 ₫`, `1.250,50 USD`, same rate line |

The browser rendering of the same payloads matched the node output character for character.

## Responsive

- 360px: checked in the app's browser pane with a true 360×800 viewport. Document and catalog
  `scrollWidth` were both 360 on `/catalog/money` and `/catalog/forms`; long lines (conversion trace,
  UTC disclosure, reasons) wrap. Headless Chrome was tried for full-page 360 captures and discarded:
  it lays the page out wider than 360 and crops, so those images would have misrepresented the layout.
- 1440px: full-page headless capture `catalog-money-1440.png`, plus `live-api-1440.jpg` from the pane.

## Gates

`npm run lint`, `npm run typecheck` and `npm run build:web` pass (`/catalog/money (55KB)` exported).
The existing jest suite passes, 514/514, after registering the new `components/value` directory in
the kit-boundary conformance test, whose purpose is to make a new directory an explicit decision.

## Contract gaps found (not patched)

1. **Browsers cannot send a bearer token cross-origin.** `shared/api/CorsConfiguration.java` allows
   request headers `Content-Type` and `X-Correlation-Id` only, so the preflight for
   `Authorization` answers `403`. Expo web on its own origin cannot call any authenticated route
   until this changes. Owner: identity #41 / typed client #38. Verification here went through a
   scratchpad-only CORS proxy on port 8281.
2. **`application-local.yml` hard-codes `allowed-origins: http://localhost:8081`**, which overrides
   `API_ALLOWED_ORIGINS`. Running web on another port needs
   `--rikkaus.api.allowed-origins=…` on the command line.
3. **No product freshness threshold.** The backend makes no stale judgement and no N-day rule is
   approved. `FreshnessBadge` therefore requires `maxAgeDays` from its caller; the catalog's 30 days is
   labelled as an example.

## Housekeeping

The API, Expo web server and proxy started for this run were stopped. The staged token file was
deleted and the browser session key cleared. The test user and its three assets remain in the
shared local database.
