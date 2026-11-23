# GH-44 — Money, currency, exchange-rate & time primitives: test cases

Written before implementation. No unit tests on this project (Product Owner direction); every case
below is executed by hand and its evidence recorded in
`plans/reports/manual-verification-261122-1629-GH-44-money-time-primitives.md`.

Execution routes:

- **F** — formatter/parser case, run through `node` against the compiled module
  (`npx tsc` of `src/money` and `src/time` into the scratchpad) and the console output recorded.
- **C** — catalog case, opened at `http://localhost:8092/catalog/money` at 360px and 1440px; the
  screenshot and the DOM text are the evidence.
- **A** — live API case, real `GET/PUT /api/v1/settings/exchange-rate` and `GET /api/v1/assets`
  against the local Postgres, then the same payload rendered through the primitives.

Clock for every time case: `Asia/Ho_Chi_Minh` (UTC+7, no daylight saving).

## 1. Decimal intake (no binary-float arithmetic)

| # | Input | Expected | Route |
|---|---|---|---|
| D1 | JSON number `12002000000` | decimal string `12002000000` | F |
| D2 | JSON number `45000000.5` (API wrote `45000000.50`) | `45000000.5` | F |
| D3 | string `"25100.000000"` | `25100.000000` kept verbatim | F |
| D4 | number `-1245000000` | `-1245000000` | F |
| D5 | number `0` and `-0` | `0` for both | F |
| D6 | `null` / `undefined` | `null` (unavailable, never `0`) | F |
| D7 | number `1e-7` | `0.0000001` (no exponent notation) | F |
| D8 | number `NaN`, `Infinity` | `null` | F |
| D9 | number `9007199254740993` (above 2^53) | flagged `precisionAtRisk: true` | F |
| D10 | string `"12,5"` / `"abc"` | rejected (`null`) — API strings are canonical | F |

## 2. Money display

| # | Input | Expected text | Expected a11y label | Route |
|---|---|---|---|---|
| M1 | `12002000000` VND | `12.002.000.000 ₫` | `12.002.000.000 đồng` | F, C |
| M2 | `380000000.00` VND | `380.000.000 ₫` (no `,00`) | `380.000.000 đồng` | F |
| M3 | `-1245000000` VND | `-1.245.000.000 ₫` | `âm 1.245.000.000 đồng` | F, C |
| M4 | `0` VND | `0 ₫` (a real zero, not unavailable) | `0 đồng` | F, C |
| M5 | `null` VND | `Không khả dụng` with reason | `Không khả dụng. <reason>` | F, C |
| M6 | `20000` USD | `20.000 USD` (never `$`) | `20.000 đô la Mỹ` | F, C |
| M7 | `1250.5` USD | `1.250,50 USD` | `1.250,50 đô la Mỹ` | F, C |
| M8 | `1250.05` USD | `1.250,05 USD` | | F |
| M9 | `0.005` USD | `0,01 USD` (half-up on the string, no float) | | F |
| M10 | `1234567.5` VND | `1.234.568 ₫` (half-up, display only) | | F |
| M11 | `-0.4` VND | `0 ₫` (no `-0 ₫`) | | F |
| M12 | `99999999999999999.99` VND (17 integer digits, as a string) | `100.000.000.000.000.000 ₫` exactly | | F |

## 3. Money input parsing (Vietnamese format)

| # | Typed | Currency/options | Expected | Route |
|---|---|---|---|---|
| P1 | `12.002.000.000` | VND | `12002000000` | F, C |
| P2 | `1.250,50` | USD | `1250.50` | F, C |
| P3 | `1250,5` | USD | `1250.5` | F |
| P4 | ` 380 000 000 ₫` (spaces, unit) | VND | `380000000` | F |
| P5 | `-1.245.000.000` | VND, negative not allowed | error `negative-not-allowed` | F, C |
| P6 | `-1.245.000.000` | VND, `allowNegative` | `-1245000000` | F |
| P7 | `1.25.000` | VND | error `invalid-format` (bad grouping) | F |
| P8 | `1,2,3` | USD | error `invalid-format` | F |
| P9 | `1.250,505` | USD | error `too-many-decimals` (max 2) | F, C |
| P10 | `abc` / `` | VND | `invalid-format` / `empty` | F |
| P11 | `123.456.789.012.345.678` (18 integer digits) | VND | error `too-large` (max 17, `NUMERIC(19,2)`) | F |
| P12 | `25.100,5` | rate (max 6 decimals) | `25100.5` | F |
| P13 | `1.250` | VND | `1250` (dot is grouping, never decimal) | F |
| P14 | Form kit: `CurrencyField` typed `12002000000` shows `12.002.000.000`, `NumberField` `1250,50` | existing callers | unchanged behaviour | C |

## 4. Exchange rate & conversion

| # | Input | Expected | Route |
|---|---|---|---|
| R1 | rate `25100.000000`, date `2026-09-12` | `1 USD = 25.100 VND` and `hiệu lực 12/09/2026` together | F, C |
| R2 | rate `25100.5` | `1 USD = 25.100,5 VND` | F |
| R3 | rate `null` (no declaration) | `Chưa có tỷ giá` + action, never a number | F, C |
| R4 | `20000` USD × `25100` | `502000000.00` → `502.000.000 ₫`, trace keeps rate + date | F, C |
| R5 | `1250.50` USD × `25100.5` | `31388175.25` exactly (BigInt) | F |
| R6 | `0.01` USD × `25100.123456` | `251.00` (half-up at 2 decimals) | F |
| R7 | USD amount with rate `null` | conversion `null`, reason `Chưa có tỷ giá USD/VND` | F, C |
| R8 | VND amount | no conversion, value passes through | F |
| R9 | live `GET /settings/exchange-rate` | rendered rate + effective date + `cập nhật` timestamp | A |
| R10 | live `PUT` `{usdToVndRate: 25100.000000, effectiveDate: "2026-09-12"}` then `GET` | `1 USD = 25.100 VND · hiệu lực 12/09/2026` | A |

## 5. Percentages

| # | Input | Expected | Route |
|---|---|---|---|
| Q1 | `18.4` | `18,4%`, a11y `18,4 phần trăm` | F, C |
| Q2 | `18.36` (1 decimal) | `18,4%` | F |
| Q3 | `0` | `0%` | F |
| Q4 | `-2.5` | `-2,5%` | F |
| Q5 | `null` with reason `Tổng tài sản bằng 0` | `Không khả dụng` + reason; never `0%`, `∞`, blank | F, C |
| Q6 | `100` | `100%` | F |

## 6. Dates, timestamps, Asia/Ho_Chi_Minh day boundaries

| # | Input | Expected | Route |
|---|---|---|---|
| T1 | date-only `2026-09-12` | `12/09/2026`, identical in any device time zone | F, C |
| T2 | instant `2026-09-13T02:30:00Z` | `09:30, 13/09/2026 (GMT+7)` | F, C |
| T3 | instant `2026-09-12T16:59:59Z` | civil date `2026-09-12` | F |
| T4 | instant `2026-09-12T17:00:00Z` | civil date `2026-09-13` (UTC still says 12th) | F |
| T5 | instant `2026-12-31T17:30:00Z` | `00:30, 01/01/2027 (GMT+7)` (year rollover) | F |
| T6 | `updatedAt` with micro/nanos `2026-11-22T09:38:02.123456Z` | `16:38, 22/11/2026 (GMT+7)` | F, A |
| T7 | relative, now `2026-09-13T04:30:00Z` vs T2 | `Khoảng 2 giờ trước`, always beside the absolute | F, C |
| T8 | relative 30 s | `Vừa xong`; 45 min `45 phút trước`; previous civil day `Hôm qua`; 10 days `10 ngày trước` | F |
| T9 | invalid `2026-02-30` / `not-a-date` | `null` → `Không khả dụng`, no crash | F |
| T10 | source disclosure | shows `UTC: 2026-09-13T02:30:00Z`, `Múi giờ: Asia/Ho_Chi_Minh` | C |

## 7. Freshness

N is the caller's: the product has not fixed a stale-rate threshold (contract: "No stale-rate rule").

| # | Input | Expected | Route |
|---|---|---|---|
| S1 | effective `2026-09-12`, today `2026-09-13`, max 30 days | `fresh` — `Còn hiệu lực`, `hiệu lực 12/09/2026` | F, C |
| S2 | effective `2026-09-12`, today `2026-11-22`, max 30 days | `stale`, 71 days — `Dữ liệu cũ`, icon + text + `12/09/2026` + update action | F, C |
| S3 | boundary: exactly 30 days old, max 30 | `fresh`; 31 days → `stale` | F |
| S4 | `today` computed across 23:30 GMT+7 (`16:30Z`) | uses Vietnamese civil date, not UTC | F |
| S5 | effective date `null` | `missing` — `Chưa có dữ liệu`, never fresh | F, C |
| S6 | live rate from `GET`, today in VN | badge reflects real age of the declared date | A |

## 8. Unavailable values

| # | Input | Expected | Route |
|---|---|---|---|
| U1 | `UnavailableValue` with reason | `Không khả dụng` + `—` shape + reason text, a11y reads both | C |
| U2 | the same at 360px | no horizontal scroll, reason wraps | C |

## 9. Live asset list through the primitives

| # | Input | Expected | Route |
|---|---|---|---|
| L1 | `GET /api/v1/assets` with one VND and one USD asset created via `POST` | VND shows `₫`; USD shows `USD` original + converted VND with rate and effective date | A |
| L2 | `currentValue` arrives as a JSON number | converted through `fromApiNumber` with no arithmetic; `precisionAtRisk` false | A |

## 10. Regression of existing callers

| # | Surface | Expected | Route |
|---|---|---|---|
| G1 | `/catalog/forms` currency, number and date fields | type, format and parse as before | C |
| G2 | `/catalog/data` amounts | still render Vietnamese grouping with `₫` | C |
| G3 | `npm run lint`, `npm run typecheck`, `npm run build:web` | pass | CLI |
