---
title: "Issue #98 validation, empty, loading & error-state hardening (Backend)"
description: "Chuẩn hóa validation đầu vào và problem payload trên mọi module ghi: không còn 500 từ PostgreSQL, không echo giá trị gửi lên, cùng một câu message cho cùng một kiểu lỗi; nâng phần làm sạch chuỗi/ngày dùng chung lên shared/validation."
status: completed
priority: P1
effort: "1.5d"
issue: 98
branch: claude/issue-98-validation-hardening-1873cf
tags: [backend, api, validation, errors, privacy]
blockedBy: []
blocks: []
created: 2026-11-24
---

# Issue #98 validation hardening (Backend)

## Outcome

Pilot user nhập `-5000-01-01`, `0000-01-31`, `"currency": 1`, một tên chỉ gồm `U+00A0` hay một tên
hiển thị chứa `U+0000` thì backend luôn trả một lỗi 4xx đúng taxonomy, có tên trường khi bean validation
hoặc service đọc được record, không bao giờ 500 từ PostgreSQL, không bao giờ lưu một giá trị khác với giá
trị đã gửi, và không echo giá trị đó trong `detail` hay `errors[].message`. Một kiểu lỗi có đúng một câu
message trên mọi module.

## Constraints

- Spring Boot 4 / Java 25 modular monolith; slice chỉ phụ thuộc `shared` (`ArchitectureRulesTest`).
- Theo chỉ đạo Product Owner: **không viết unit test/integration test mới**. Phase 1 là ma trận kịch bản
  negative-input viết trước khi sửa code; verification là chạy thật qua HTTP vào PostgreSQL 18.6 Docker,
  trước và sau khi sửa. `OpenApiContractIT`, `ApplicationContextIT`, `ArchitectureRulesTest` vẫn xanh.
- Không migration (V14 cao nhất). Không đổi contract đọc (GET), không đổi shape/sort/filter của các list
  `GET` (#107 đang audit), không sửa slice `snapshot` (#68), không thêm fixture script dưới
  `services/api/fixtures/`.
- Không đổi ý nghĩa nghiệp vụ của rule đã có. Mọi thay đổi ý nghĩa ghi vào *Câu hỏi cho PO*.
- Design #96 của feature chưa được PO duyệt; mọi giả định ghi vào *Câu hỏi cho PO*.

## Non-goals

- Chuyển lỗi Jackson (enum lạ, ngày sai dạng, sai kiểu JSON) từ `malformed-request` sang
  `validation-failed` có field. Docs đã công bố `malformed-request` cho trường hợp này và client đã map
  theo `type`. Xem câu hỏi PO #1.
- Thiết kế lại state UI, loading/empty (thuộc #96/#97 và MVP 0 #116/#118).
- Giới hạn nghiệp vụ mới cho ngày xa (ví dụ `targetDate` tối đa 100 năm). Xem câu hỏi PO #2.

## Quyết định

| Quyết định | Giá trị | Căn cứ |
|---|---|---|
| Ngày trong JSON body | Chỉ nhận chuỗi `YYYY-MM-DD`, năm 4 chữ số từ `0001` đến `9999`, ngày có thật; số (epoch day), mảng `[2026,9,12]`, chuỗi date-time, năm `0000`, năm âm hay 5+ chữ số là `malformed-request`. Chuỗi rỗng `""` vẫn là `null` như trước | Đo trên `main`: `-5000-01-01` → 500; `0000-01-31` lưu thành `0001-01-31 BC` và đọc lại `0001-01-31`; `20000` → `2024-10-04`; `"2026-09-12T20:00:00Z"` → ngày UTC `2026-09-12` trong khi ở Việt Nam đã là 13/09 |
| Enum trong JSON body | Số nguyên không còn được hiểu là chỉ số enum (`"currency": 1` → `malformed-request`) | Đo trên `main`: `"currency": 1` tạo khoản nợ **USD** 201, `"category": 0` tạo `BANK_LOAN` |
| Chuỗi | Một định nghĩa ở `shared/validation/TextInput`: bỏ khoảng trắng hai đầu theo nghĩa Unicode (Java whitespace, mọi `Zs`, thêm `U+200B`, `U+2060`, `U+FEFF`), từ chối `U+0000` bằng `must not contain a null character`, chuỗi rỗng sau khi làm sạch là `must not be blank` (bắt buộc) hoặc `null` (tùy chọn) | Năm slice tự viết lại cùng một vòng lặp; profile dùng `trim()` nên `U+00A0` được lưu và `U+0000` thành 500 |
| Ngày Việt Nam | `shared/validation/EnteredDates.today(clock)` đọc hôm nay ở `Asia/Ho_Chi_Minh` | Bốn service tự khai báo cùng một `ZoneId` |
| Message | Mọi annotation có `message` tường minh; locale của request cố định `en` | Settings và profile dùng message mặc định của Hibernate Validator, bị dịch theo `Accept-Language` (`de` → `muss größer 0 sein`) |
| Message lệch cùng kiểu lỗi | Forecast `from` dùng `must be a calendar month in the form YYYY-MM` như report | Hai câu khác nhau cho cùng một lỗi tháng |
| `InputRules` của assets | Giữ, chỉ còn rule riêng của assets (snapshot tỷ giá); phần chung chuyển sang `shared/validation` | Brief của coordinator |

## Phases

| # | Phase | File | Status |
|---|---|---|---|
| 1 | Ma trận kịch bản negative-input | [phase-01-test-scenarios.md](phase-01-test-scenarios.md) | completed |
| 2 | `shared/validation` và các slice | [phase-02-shared-validation-and-slices.md](phase-02-shared-validation-and-slices.md) | completed |
| 3 | Contract, docs, chạy ma trận trước/sau | [phase-03-contract-docs-verification.md](phase-03-contract-docs-verification.md) | completed |

## Acceptance criteria

- Mọi kịch bản ở phase 1 pass trên commit cuối, chạy qua HTTP vào PostgreSQL 18.6 Docker; kịch bản đánh
  dấu `[FIX]` có response trước/sau trong report.
- Không response nào trong lần chạy sau khi sửa là 500; không body lỗi nào chứa giá trị bị từ chối, tên
  lớp Java, SQL hay tên cột.
- `clean verify` xanh không flag; `openapi.json` tái tạo; client mobile tái tạo nếu contract đổi.

## Câu hỏi cho PO

Ghi đầy đủ, kèm ví dụ, ở report
`plans/reports/manual-verification-261124-1003-GH-98-validation-hardening.md` và trong nội dung PR.
