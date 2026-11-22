---
title: "Issue #101 search & basic filters"
description: "Thêm tham số query tùy chọn q, filter enum nhiều giá trị, khoảng effectiveDate và sort cho bốn list endpoint assets, liabilities, cash-flow-entries, goals; không tham số thì giữ nguyên hành vi."
status: completed
priority: P1
effort: "1.5d"
issue: 101
branch: thientrinhcoder/feat/issue-101-search-basic-filters
tags: [backend, api, search, filters]
blockedBy: []
blocks: [100]
created: 2026-11-22
---

# Issue #101 search & basic filters

## Outcome

Người dùng pilot gõ `thao dien` vào ô tìm kiếm và thấy `Căn hộ Thảo Điền`; chọn chip `Tiền mặt` và
`Tài khoản ngân hàng` cùng lúc và chỉ thấy hai loại đó; xem thu chi của tháng 09/2026; xóa bộ lọc và
thấy lại toàn bộ danh sách. Bốn list endpoint hiện có nhận tham số query tùy chọn; không gửi tham số
nào thì body giống từng byte với `main` (`abadb96`). Ownership luôn áp trước mọi filter: cùng một `q`,
user B không bao giờ thấy record của A.

## Constraints

- Spring Boot modular monolith, prefix `/api/v1`, lỗi RFC 9457 theo `ProblemType`, owner chỉ từ
  `CurrentUser` (ArchUnit cấm bind `userId`/`ownerId` từ request).
- Slice chỉ phụ thuộc `shared`. Phần dùng chung (đọc `q`, parse enum nhiều giá trị, parse ngày, parse
  `sort`, predicate tìm chữ) nằm ở `shared/search`; truy vấn của từng slice nằm trong slice đó.
- Truy vấn bằng JPA Criteria có tham số trong một repository fragment của từng slice
  (`AssetSearch`/`AssetSearchImpl`, ...). Không nối chuỗi SQL. Điều kiện `user_id = :owner` được dựng
  bên trong fragment, không có tham số nào để bỏ qua nó.
- Theo chỉ đạo Product Owner: **không viết unit test/integration test mới**. Phase 1 là danh sách
  scenario viết trước khi code; verification là chạy thật qua HTTP vào PostgreSQL 18.6 Docker. Inventory
  `OpenApiContractIT`, `ApplicationContextIT` và drift check `openapi.json` vẫn phải xanh (CI gate).
- Chỉ sửa controller/service/repository list của bốn slice và `shared/search` mới; không đụng
  `cashflow/reporting` (#77), `cashflow/forecast` (#80). Giữ nguyên các method repository mà code
  khác có thể gọi (`findActiveBetween`, `findById`, ...).
- Design #99 chưa được PO duyệt. Contract chốt từ backbone và các review HTML trong `docs/`; giữ nhỏ.

## Non-goals

- Full-text search (`tsvector`), xếp hạng độ liên quan, gợi ý chính tả.
- Phân trang (conventions ghi là cố ý chưa có), filter đa domain trong một route, lưu bộ lọc.
- Filter theo trạng thái mục tiêu (`Đúng kế hoạch` / `Có rủi ro` / `Chậm kế hoạch` trong review goals):
  trạng thái là kết quả projection, không phải cột lưu. Xem câu hỏi PO #3.
- Tìm cash-flow theo nhãn danh mục tiếng Việt hoặc tên tài sản/khoản nợ liên kết (placeholder
  `Tìm theo danh mục hoặc liên kết` trong review cash-flow). Xem câu hỏi PO #2.
- Sort theo số tiền: các bản ghi trộn VND và USD, so sánh số trần sẽ sai; quy đổi là việc của client.

## Quyết định contract

| Quyết định | Giá trị | Căn cứ |
|---|---|---|
| Hình dạng response | Không đổi: vẫn là mảng của `AssetResponse`/`LiabilityResponse`/`CashFlowEntryResponse`/`GoalResponse`, không envelope | Body không tham số phải giống từng byte; client #100 không phải đổi parser |
| `q` | Tùy chọn. Bỏ khoảng trắng đầu/cuối (`String.strip`); rỗng sau khi strip = không lọc (reset) | Ô tìm kiếm bị xóa thường gửi `q=` |
| Trường `q` tìm | assets: `name`, `institution`, `notes`; liabilities: `name`, `notes`; cash-flow: `notes` (entry không có `name`); goals: `name` (goal không có `notes`) | Đúng các cột text tự do của từng bảng |
| Cách khớp | Chuỗi con, không phân biệt hoa thường, **bỏ dấu tiếng Việt**: `thao dien`, `THẢO ĐIỀN`, `Thảo Điền` đều khớp `Căn hộ Thảo Điền`; `đ`/`Đ` khớp `d` | Người dùng hay gõ không dấu trên điện thoại |
| Cách bỏ dấu | `lower(translate(col, <134 chữ có dấu + 8 dấu tổ hợp>, <chữ gốc>))` áp **cả hai phía** (cột và mẫu). Không dùng extension `unaccent` | `translate` là hàm lõi, immutable, không cần `CREATE EXTENSION` trên host managed; bao phủ đúng bảng chữ Việt, cả dạng NFC và NFD |
| Ký tự đặc biệt trong `q` | `%`, `_`, `\` là chữ thường, được escape; `q=%` không khớp mọi thứ | Không để người dùng vô tình gửi wildcard |
| Giới hạn `q` | ≤ 120 ký tự (code point, sau strip); chứa U+0000 → `validation-failed` field `q` | Bằng giới hạn `name`; U+0000 PostgreSQL không lưu được |
| Filter enum | assets: `category`, `currency`, `liquidity`; liabilities: `category`, `currency`; cash-flow: `entryType`, `category`, `timing`, `recurrence`, `currency`; goals: `type` | Tên tham số = tên field trong response (`entryType`, không phải `type`) |
| Nhiều giá trị | Lặp tham số `category=CASH&category=BANK_ACCOUNT` hoặc phẩy `category=CASH,BANK_ACCOUNT`. Trong một tham số là OR, giữa các tham số là AND | Spring tách phẩy sẵn khi bind `List<String>`; ghi rõ cả hai dạng |
| Giá trị enum | Đúng tên hằng, phân biệt hoa thường (`CASH`, không nhận `cash`). Giá trị rỗng bị bỏ qua (`category=` = không lọc). Giá trị lạ → `validation-failed` field = tên tham số, message liệt kê giá trị hợp lệ, không lặp lại input | Một kiểu lỗi cho client; không phản chiếu input |
| `timing` cho cash-flow | `ONE_TIME` / `RECURRING` | Sheet `Bộ lọc cơ bản` trong review cash-flow có `Thời điểm: Tất cả / Một lần / Định kỳ` |
| Khoảng ngày cash-flow | `effectiveFrom`, `effectiveTo`, `YYYY-MM-DD`, bao gồm hai đầu, mỗi đầu tùy chọn. Lọc trên `effectiveDate` lưu trữ, không mở rộng lịch định kỳ. `effectiveTo < effectiveFrom` → `validation-failed` field `effectiveTo` | Cùng parser chặt như `OccurrenceWindow`; ngày định kỳ theo tháng là việc của `/cash-flow-occurrences` |
| `sort` | Một giá trị; `-` đầu là giảm dần. assets: `-valuationDate` (mặc định), `valuationDate`, `name`, `-name`. liabilities: `nextPaymentDate` (mặc định), `-nextPaymentDate`, `name`, `-name`. cash-flow: `-effectiveDate` (mặc định), `effectiveDate`. goals: `targetDate` (mặc định), `-targetDate`, `name`, `-name`. Khóa lạ hoặc nhiều giá trị → `validation-failed` field `sort` | Vài khóa rõ ràng; mặc định đúng thứ tự hiện tại |
| Tie-break | Giữ khóa phụ hiện tại (name / createdAt), thêm `id` cuối cùng | "deterministic sorting" của issue; chỉ xác định lại thứ tự vốn chưa xác định khi trùng hoàn toàn |
| Ngày null khi sort | `nextPaymentDate` null luôn ở cuối, cả hai chiều | Như thứ tự hiện tại |
| Nhiều lỗi | Mọi tham số được kiểm tra cùng lúc; một response liệt kê hết | Như `OccurrenceWindow` |
| Tham số không biết | Bị bỏ qua (`?page=2` trả như không có) | Hành vi mặc định Spring; pagination vẫn ngoài phạm vi |
| Migration / index | **Không có `V13`.** Truy vấn đi qua index `(user_id, ...)` hiện có; `LIKE '%x%'` không dùng được btree và `pg_trgm` cần extension. Một user pilot có vài chục dòng | Ticket chỉ cho phép migration khi cần index |

## Phases

| # | Phase | File | Status |
|---|---|---|---|
| 1 | Danh sách test scenario | [phase-01-test-scenarios.md](phase-01-test-scenarios.md) | completed |
| 2 | `shared/search` và bốn slice | [phase-02-shared-and-slices.md](phase-02-shared-and-slices.md) | completed |
| 3 | Contract, inventory, docs | [phase-03-contract-and-docs.md](phase-03-contract-and-docs.md) | completed |
| 4 | Chạy scenario thật, ghi evidence | [phase-04-manual-verification.md](phase-04-manual-verification.md) | completed |

## Acceptance criteria

- Mọi scenario ở phase 1 pass trên commit cuối, chạy qua HTTP vào PostgreSQL 18.6 Docker; case nào fail
  rồi được sửa thì ghi là đã fail.
- Bốn list endpoint không tham số trả body giống từng byte với jar build từ `main` trên cùng database.
- `clean verify` xanh không flag; `openapi.json` regenerate, mỗi tham số có mô tả và ví dụ.
- `docs/api-contract-conventions.md` có mục search & filters; `docs/manual-test-guide-search-filters.md`
  chạy lại được; report evidence ở `plans/reports/`.

## Verification gap

- Không có automated test nào chạm tham số tìm kiếm/lọc (chỉ đạo PO). Regression chỉ bị bắt khi chạy
  lại `docs/manual-test-guide-search-filters.md`.
- Bỏ dấu chỉ bao phủ bảng chữ tiếng Việt. Dấu của ngôn ngữ khác (`ñ`, `ü`, `ç`) vẫn phải gõ đúng;
  chữ hoa ngoài ASCII và tiếng Việt phụ thuộc `lower()` của locale database.
- Sort theo `name` dùng collation mặc định của database, như tie-break hiện tại; thứ tự `Đ` so với `D`
  phụ thuộc collation của môi trường triển khai, chưa đối chiếu với AWS RDS.
- Chưa đo hiệu năng với catalog lớn; không có index cho tìm chuỗi con.
- Chưa chạy cùng app mobile (#100) và chưa đối chiếu design #99 (chưa duyệt).
- Thứ tự key `field`/`message` trong mỗi phần tử `errors` đổi giữa hai lần khởi động JVM (`Map.of`), như
  mọi slice hiện có. Client đọc theo tên nên không ảnh hưởng; body lỗi không ổn định từng byte.
- Chỉ chạy trên PostgreSQL 18.6 Alpine (libc `en_US.utf8`); `lower()` với chữ hoa tiếng Việt đã kiểm
  trên môi trường này, nhưng bảng `translate` không phụ thuộc locale nên phần bỏ dấu giống nhau ở mọi nơi.

## Kết quả

96 scenario (152 lượt kiểm tra) pass trên commit `ab1af04` và lại pass sau khi merge `main` `becca58`; lần đầu 140/152 vì scenario X03–X05 ghi sai
type 401, đã sửa scenario. Evidence:
[`plans/reports/manual-verification-261122-1538-GH-101-search-basic-filters.md`](../reports/manual-verification-261122-1538-GH-101-search-basic-filters.md).

## Câu hỏi cho PO / design #99

1. Bỏ dấu khi tìm (`thao dien` khớp `Thảo Điền`) có đúng ý không, hay cần khớp đúng dấu? Đổi là bỏ
   `translate` trong một hàm của `shared/search`.
2. Ô tìm cash-flow trong review ghi `Tìm theo danh mục hoặc liên kết`. Backend hiện chỉ tìm trong
   `notes`; nhãn danh mục tiếng Việt (`Học phí`, `Lương`) do client giữ. Client lọc nhãn tại chỗ, hay
   backend cần bảng nhãn và tìm theo tên tài sản/khoản nợ liên kết?
3. Chip trạng thái mục tiêu (`Đúng kế hoạch` / `Có rủi ro` / `Chậm kế hoạch`) cần filter server-side
   không? Trạng thái được tính từ projection, không lưu, nên hiện client tự lọc.
4. Tên tham số cash-flow là `entryType` (trùng field response) thay vì `type`. Có cần alias `type`?
5. Sau #56, tài sản có thêm `symbol` (mã chứng khoán/quỹ, ví dụ `E1VFVN30`). `q` hiện chưa tìm trong
   `symbol`; có muốn gõ `fpt` để ra `Cổ phiếu FPT` theo mã không? Thêm là một cột trong `AssetSearchImpl`.
