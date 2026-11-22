---
title: "Issue #80 dự báo dòng tiền ba tháng"
description: "Service và route đọc GET /api/v1/cash-flow-forecast trong slice cashflow: ba tháng liên tiếp từ entry định kỳ, entry một lần và lịch trả nợ hiển thị được, số dư mở đầu từ tiền mặt và tài khoản ngân hàng, tín hiệu số dư thấp nhất, mọi khoản truy về source. Tính khi đọc, không migration."
status: completed
priority: P1
effort: "2d"
issue: 80
branch: thientrinhcoder/feat/issue-80-three-month-forecast
tags: [backend, api, cash-flow, forecast]
blockedBy: [74]
blocks: [79, 83]
created: 2026-11-22
---

# Issue #80 dự báo dòng tiền ba tháng

## Outcome

Người dùng pilot hỏi được "ba tháng tới tiền của tôi đi về đâu, và khi nào tôi thấp nhất" qua
`GET /api/v1/cash-flow-forecast?from=YYYY-MM`. Mỗi tháng có thu dự kiến, chi dự kiến, net, số dư đầu và
cuối tháng, điểm thấp nhất trong tháng và danh sách khoản mục; mỗi khoản mục mang `sourceType` và
`sourceId` mà `GET` của slice gốc đọc lại được. Response có tín hiệu số dư thấp nhất của cả horizon,
danh sách nguồn bị loại kèm lý do, và `assumptions` (ngày tính, múi giờ, tỷ giá, quy tắc số dư mở đầu).
Kết quả xác định: cùng dữ liệu, cùng ngày, cùng `from` cho cùng body từng byte. Đây là đầu vào cho
frontend #79 và chỉ số "Forecast minimum cash balance" của #83.

## Constraints

- Mọi file mới nằm trong `com.rikkaus.wealth.cashflow.forecast` (`api`, `application`). Không sửa file
  hiện có của `cashflow`: `CashFlowOccurrenceService.occurrencesFor` và `OccurrenceWindow` đã `public`, nên
  tái dùng được nguyên trạng. `CashFlowEntryRepository.findActiveBetween` (đã `public`) dùng cho quy tắc
  khử trùng lặp.
- Không import slice `assets`, `liabilities`, `settings`: đọc `wealth.assets`, `wealth.liabilities`,
  `wealth.user_settings` bằng SQL native qua `JdbcClient`, luôn lọc theo `user_id` lấy từ `CurrentUser`.
- Tiền là `BigDecimal` qua `shared/money` (`Money`, `ExchangeRate`, `ConversionTrace`); không float.
- Không migration, không bảng mới, không thêm `ProblemType` vào `shared` (tránh đụng #77 và #101).
- Không unit test/integration test mới (chỉ đạo Product Owner). Bù bằng scenario viết trước ở phase 1,
  chạy thật qua HTTP vào PostgreSQL Docker. Inventory `OpenApiContractIT` được mở rộng vì là CI gate.

## Non-goals

Báo cáo tháng quá khứ (#77), chỉ số sức khỏe tài chính (#83), insight, what-if, horizon khác ba tháng,
sửa recurrence, ngưỡng do người dùng tự đặt, cảnh báo "tỷ giá cũ" theo tuổi tỷ giá.

## Quyết định đã chốt

Design đã duyệt: [`docs/three-month-cash-flow-forecast-review.html`](../../docs/three-month-cash-flow-forecast-review.html)
và plan design #78 (`plans/260920-1437-issue-78-three-month-cash-flow-forecast/`, ở checkout chính). Design
nói rõ nó không định nghĩa thuật toán production; các điểm design để lại được chốt như sau.

| # | Quyết định | Nguồn | Ví dụ |
|---|---|---|---|
| D1 | Route `GET /api/v1/cash-flow-forecast`, tham số tùy chọn `from=YYYY-MM`; vắng mặt thì là tháng kế tiếp theo `Asia/Ho_Chi_Minh` | design: "ba tháng trọn vẹn kế tiếp"; brief | ngày 22/11/2026, không `from` → 12/2026–02/2027 |
| D2 | Horizon luôn ba tháng lịch trọn vẹn, `[ngày 1 tháng from, ngày cuối tháng from+2]` | design, quy tắc horizon | `from=2026-12` → 01/12/2026–28/02/2027 |
| D3 | `from` phải từ tháng kế tiếp đến tối đa 12 tháng sau tháng hiện tại; tháng hiện tại và quá khứ bị từ chối | #80: số dư mở đầu là số dư đang ghi nhận, đã phản ánh các khoản trong tháng hiện tại | ngày 22/11/2026: `2026-11` → 400, `2027-11` được, `2027-12` → 400 |
| D4 | Khoảng từ ngày sau ngày tính đến trước ngày đầu horizon **không được chiếu**; response nêu khoảng đó trong `assumptions.notProjected` | design: "tháng hiện tại dang dở không thuộc dự báo" | 23/11/2026–30/11/2026 |
| D5 | Nguồn khoản mục: kỳ của entry định kỳ (qua expansion #74), entry **một lần** có ngày trong horizon, và kỳ trả của công nợ | design quy tắc 1: mọi bản ghi người dùng đang thấy; entry một lần cũng là bản ghi hiển thị | `Sửa xe 8.500.000 ₫` ngày 18/01/2027 vào tháng 01/2027 |
| D6 | Lịch trả nợ: cần đủ `recurringPaymentAmount`, `recurringPaymentFrequency`, `nextPaymentDate`; kỳ thứ k là `nextPaymentDate + k × bước` (tháng 1, quý 3, nửa năm 6, năm 12), k ≥ 0, không sau `maturityDate`; ngày neo không có trong tháng thì dời về cuối tháng như #74 | brief; design quy tắc 7 | `Vay mua xe VPBank` kỳ 10/12/2026, 10/01/2027; đáo hạn 10/01/2027 nên không có kỳ 02/2027 |
| D7 | `nextPaymentDate` trong quá khứ vẫn là ngày neo; kỳ trước horizon không được cộng dồn (khoản quá hạn không phải dự báo) | #80 | neo 15/10/2026 → kỳ 15/12, 15/01, 15/02 |
| D8 | Khử trùng lặp: công nợ có ít nhất một entry **định kỳ** liên kết (`linkedLiabilityId`) còn hiệu lực giao với horizon thì lịch trả của nó bị loại với lý do `COVERED_BY_LINKED_ENTRIES` và `linkedEntryIds`; entry liên kết vẫn vào dự báo | design quy tắc 2–3 | `Vay sửa nhà BIDV 7.000.000 ₫/tháng` bị loại vì entry `Trả nợ vay sửa nhà` |
| D9 | Công nợ thiếu lịch → loại với `NO_PAYMENT_SCHEDULE` (hoặc `NO_NEXT_PAYMENT_DATE`); nếu `maturityDate` rơi trong horizon thì tháng đó `partial: true` và lý do mang `affectedMonths` | design quy tắc 5 | `Vay người thân 50.000.000 ₫` đáo hạn 31/01/2027 → 01/2027 partial |
| D10 | Công nợ có lịch nhưng không kỳ nào trong horizon → `NO_PAYMENT_IN_HORIZON`; entry định kỳ giao horizon mà không kỳ nào rơi vào → `NO_OCCURRENCE_IN_HORIZON` | design: "không nghĩa vụ nào biến mất không lời giải thích" | `Phí bảo hiểm nhân thọ` năm, kỳ kế 15/06/2027 |
| D11 | Số dư mở đầu = tổng `current_value` của tài sản category `CASH` và `BANK_ACCOUNT`, bất kể `liquidity`; áp vào đầu ngày 1 của horizon | design: "tiền mặt và tương đương tiền"; cùng định nghĩa với onboarding | `Tài khoản Vietcombank 85.000.000 ₫` + `Tiền mặt 12.000.000 ₫` + `Tài khoản USD 2.000 USD` |
| D12 | Tiết kiệm có kỳ hạn, chứng khoán… không vào số dư mở đầu dù `liquidity: HIGH` | #80: tiền gửi kỳ hạn không chi trả hằng ngày được | `Tiết kiệm kỳ hạn 6 tháng 500.000.000 ₫` không được tính |
| D13 | Quy đổi: USD × `usd_to_vnd_rate` của người dùng qua `ConversionTrace`, làm tròn đến đồng (HALF_UP, theo `Currency.VND`) **từng khoản** trước khi cộng; số tiền VND trên dây có scale 2 như catalog | backbone §7; `shared/money` | `150 USD × 25.100 = 3.765.000,00 ₫` |
| D14 | Chưa khai tỷ giá: nguồn USD (kỳ entry, kỳ công nợ, tài sản mở đầu) bị loại với `EXCHANGE_RATE_MISSING`, tháng bị ảnh hưởng `partial: true`, `assumptions.exchangeRate: null`; vẫn trả 200 | design: trạng thái giữ ngữ cảnh và cho cách khắc phục; không thêm `ProblemType` | user C có `Cổ tức 150 USD` → 12/2026 partial |
| D15 | Điểm thấp nhất xét trên điểm mở đầu và số dư **cuối mỗi ngày có khoản**; hòa thì ngày sớm nhất thắng; có `month`, `date`, `amount`, `negative` | design quy tắc 4 | B thấp nhất `-59.000.000 ₫` ngày 08/01/2027, không phải cuối tháng |
| D16 | Ngưỡng tham chiếu = chi dự kiến trung bình một tháng của horizon (scale 2, HALF_UP); `status`: `INSUFFICIENT_DATA` khi không nguồn số dư mở đầu nào được tính, `NEGATIVE` khi thấp nhất < 0, `THIN` khi < ngưỡng, còn lại `ADEQUATE` | design: bốn biến thể tín hiệu, ngưỡng "một tháng chi phí dự báo" | A: ngưỡng `78.166.666,67 ₫` |
| D17 | Thứ tự xác định: khoản mục theo ngày, rồi `CASH_FLOW_ENTRY` trước `LIABILITY`, rồi `sourceId` dạng chuỗi; nguồn loại theo `sourceType` rồi `sourceId`; nguồn mở đầu theo `sourceId` | #74 dùng cùng cách | hai lần gọi cùng sha256 |

## Contract

`GET /api/v1/cash-flow-forecast[?from=YYYY-MM]`, `bearerAuth`, owner-scoped bằng token, không path variable.

```text
horizon {from, to, months[3]}
openingBalance {amount, appliedOn, sources[{sourceType: ASSET, sourceId, category, valuationDate, amount, currency, amountVnd}]}
months[3] {month, income, expense, net, openingBalance, closingBalance, lowestBalance, lowestBalanceDate, partial,
           items[{date, direction: INCOME|EXPENSE, sourceType: CASH_FLOW_ENTRY|LIABILITY, sourceId, category,
                  amount, currency, amountVnd, cadence, sequence, shiftedToMonthEnd}]}
minimumBalance {date, month, amount, negative, referenceThreshold, status}
excludedSources[{sourceType, sourceId, reason, affectedMonths[], linkedEntryIds[]}]
partial
assumptions {asOfDate, timeZone, currency: VND, exchangeRate {usdToVndRate, effectiveDate} | null,
             openingBalanceCategories [BANK_ACCOUNT, CASH], notProjected {from, to} | null}
```

Lỗi: `400 validation-failed` (field `from`: sai dạng, trước tháng kế tiếp, quá 12 tháng), `401`, `405`, `406`.

## Phases

| Phase | Tên | Status | Kết quả |
|---|---|---|---|
| 1 | [Danh sách test scenario](./phase-01-test-scenarios.md) | Complete | 55 case với kỳ vọng tính tay, viết trước khi code |
| 2 | [Service và route forecast](./phase-02-forecast-service-and-http.md) | Complete | package `cashflow.forecast`, đọc native, tái dùng expansion |
| 3 | [Contract và docs](./phase-03-contract-and-docs.md) | Complete | `openapi.json`, `OpenApiContractIT`, `api-contract-conventions.md` |
| 4 | [Xác minh thủ công](./phase-04-manual-verification.md) | Complete | guide, report evidence, commit, push, PR body |

## Acceptance criteria

- [x] Mọi case ở phase 1 đạt trên commit cuối, ghi status và body thật.
- [x] `clean verify` xanh không flag.
- [x] Mọi khoản mục truy về source đang tồn tại; xóa source thì khoản mục biến mất ở lần đọc sau.
- [x] User B không thấy gì của A; 401 ba kiểu; 405; `from` sai bị 400 với field.
- [x] Hai lần gọi cùng input cho body giống từng byte.
- [x] `openapi.json`, `OpenApiContractIT`, `docs/api-contract-conventions.md` cập nhật.

## Verification gap

- Không automated test nào bảo vệ thuật toán; regression chỉ bị phát hiện khi chạy lại
  `docs/manual-test-guide-cash-flow-forecast.md`.
- "Hôm nay" lấy từ đồng hồ hệ thống; lần chạy thủ công chỉ kiểm được ngày 22/11/2026. Biên đổi tháng
  (chạy đúng 23:59 ngày cuối tháng giờ Việt Nam) và ngày cuối tháng (khi `notProjected` là `null`)
  không được chạy thật.
- Tần suất `SEMIANNUAL` và `YEARLY` của công nợ chỉ được suy ra từ cùng một vòng lặp với `MONTHLY`,
  không có case riêng; công nợ bằng USD không có case riêng (chung đường quy đổi với entry USD).
- Hiệu năng với catalog lớn không được đo.
- Lần chạy dùng script Python gửi cùng request với guide; các khối lệnh `bash` trong
  `docs/manual-test-guide-cash-flow-forecast.md` không được chạy nguyên văn.
- `NO_NEXT_PAYMENT_DATE` không có case riêng.

## Câu hỏi cho Product Owner / Feature Contract #22

1. `from` xa hơn tháng kế tiếp: hiện không chiếu khoảng giữa (D4) và chỉ nêu ra trong `assumptions`.
   Có cần cộng dồn khoảng đó vào số dư mở đầu không?
2. Ngưỡng tham chiếu: giữ một tháng chi dự kiến trung bình (D16) hay để người dùng tự đặt (câu hỏi mở của design)?
3. Khử trùng lặp (D8): chênh lệch giữa lịch trả và entry liên kết (design ví dụ 2.500.000 ₫/tháng) hiện
   không được tính thành số; client so được từ `linkedEntryIds`. Có cần server tính chênh lệch không?
4. "Tỷ giá cũ": khi nào một tỷ giá bị coi là cũ? Hiện chỉ trả `effectiveDate`.
5. Số dư mở đầu có nên gồm tiền gửi kỳ hạn sắp đáo hạn trong horizon không (D12)?

## Risks and rollback

Rủi ro chính là đọc dự báo như cam kết; response nêu nguồn, giả định và nguồn bị loại. Rollback là xóa
package `cashflow.forecast` và path trong contract; không có dữ liệu hay migration để hoàn tác.
