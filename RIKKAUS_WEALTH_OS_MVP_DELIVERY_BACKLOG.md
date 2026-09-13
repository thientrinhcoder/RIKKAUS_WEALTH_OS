# Rikkaus Wealth OS — MVP Delivery Backlog

> Snapshot đồng bộ từ [GitHub Project #6](https://github.com/users/thientrinhcoder/projects/6) ngày 2026-09-12. Phạm vi pilot: MVP 0 và MVP 1A–1D; Phase 2 chỉ là roadmap và không nằm trong bảng delivery này.

## Tổng quan

| Phạm vi | Epic | Feature | Task | Design | Frontend | Backend | Tổng effort | Feature dependencies |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| MVP 0 | 1 | 6 | 14 | 7d | 24d | 25d | 56d | 6 |
| MVP 1A | 1 | 7 | 21 | 17d | 27d | 31d | 75d | 19 |
| MVP 1B | 1 | 5 | 15 | 14d | 23d | 25d | 62d | 12 |
| MVP 1C | 1 | 3 | 9 | 9d | 15d | 13d | 37d | 8 |
| MVP 1D | 1 | 7 | 21 | 21d | 31d | 25d | 77d | 23 |
| **Tổng** | **5** | **28** | **80** | **68d** | **120d** | **119d** | **307d** | **68** |

### Quy ước estimation

- XS = 1, S = 2, M = 3, L = 5 ideal engineer-days.
- Task có estimate trực tiếp; Feature và Epic là tổng effort của các Task con.
- Đây là effort, không phải thời gian lịch. Design, Frontend và Backend có thể chạy song song sau khi contract của Feature ổn định.
- Chưa bao gồm contingency, thời gian chờ dependency và vòng review riêng của Product/QA.

## Kế hoạch thực thi theo thứ tự và theo team

### Mô hình phối hợp

Mỗi wave là một nhóm công việc có thể giao đồng thời cho nhiều engineer. Design và Backend bắt đầu cùng lúc; Frontend đi sau khoảng một wave và chỉ bắt đầu phần UI nghiệp vụ khi đủ hai điều kiện:

1. **Design Approved:** PO đã duyệt user flow, màn hình, responsive behavior và các trạng thái loading/empty/error liên quan.
2. **API Ready:** Backend đã chốt OpenAPI/schema, error contract, quyền sở hữu dữ liệu và cung cấp fixture hoặc endpoint chạy được.

Các task Frontend nền tảng như repository setup, application shell, CI và typed client không cần chờ toàn bộ thiết kế nghiệp vụ. Trong cùng một wave, các task được liệt kê trên cùng một hàng có thể giao cho các engineer khác nhau làm song song nếu ownership file/module tách biệt.

### Wave 0 — Repository và nền móng giao diện

| Team | Task có thể làm song song | Điều kiện bắt đầu | Kết quả/gate cần đạt |
|---|---|---|---|
| Design | [#36 Design system & responsive application shell](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/36) | Bắt đầu ngay | PO duyệt tokens, components, shell và responsive rules |
| Backend | [#35 Repository & local development foundation](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/35) | Bắt đầu ngay | Backend chạy local, cấu trúc module và cấu hình môi trường sẵn sàng |
| Frontend | [#34 Repository & local development foundation](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/34) | Bắt đầu ngay; không cần chờ Design | Frontend chạy local và có quality commands cơ bản |

### Wave 1 — Platform contract, application shell và CI/CD

| Team | Task có thể làm song song | Điều kiện bắt đầu | Kết quả/gate cần đạt |
|---|---|---|---|
| Design | [#40 Identity, session & ownership boundary](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/40)<br>[#43 Money, currency, exchange-rate & time primitives](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/43) | Design system direction đã rõ | PO duyệt auth/session flow và quy tắc hiển thị tiền, thời gian |
| Backend | [#39 API contract, errors & typed client foundation](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/39)<br>[#47 CI/CD quality gates & AWS preview baseline](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/47) | #35 hoàn tất | OpenAPI/error envelope và backend CI/preview baseline sẵn sàng |
| Frontend | [#37 Design system & responsive application shell](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/37)<br>[#46 CI/CD quality gates & AWS preview baseline](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/46) | #34 hoàn tất; #37 cần #36 được PO duyệt | Application shell và frontend CI/preview chạy được |

### Wave 2 — Identity, tiền tệ và discovery cho các domain cốt lõi

| Team | Task có thể làm song song | Điều kiện bắt đầu | Kết quả/gate cần đạt |
|---|---|---|---|
| Design | [#48 Personal profile & exchange-rate settings](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/48)<br>[#51 Asset catalog & common CRUD](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/51)<br>[#69 Cash-flow entry catalog & CRUD](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/69)<br>[#84 Generic financial goals & templates](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/84) | #36 được PO duyệt; các quyết định từ #40 và #43 đủ ổn định | PO duyệt UX của bốn domain entry-point |
| Backend | [#42 Identity, session & ownership boundary](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/42)<br>[#45 Money, currency, exchange-rate & time primitives](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/45) | #39 hoàn tất | Auth/ownership và money/time primitives có test và API contract ổn định |
| Frontend | [#38 API contract, errors & typed client foundation](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/38)<br>[#41 Identity, session & ownership boundary](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/41)<br>[#44 Money, currency, exchange-rate & time primitives](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/44) | #39 API Ready; #40/#43 Design Approved; backend cung cấp fixture | Typed client, auth UI và money/time UI tích hợp được |

### Wave 3 — Các catalog/CRUD cốt lõi

| Team | Task có thể làm song song | Điều kiện bắt đầu | Kết quả/gate cần đạt |
|---|---|---|---|
| Design | [#54 Type-specific asset details](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/54)<br>[#57 Append-only asset valuation history](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/57)<br>[#60 Liability catalog, schedules & collateral](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/60)<br>[#72 Recurring cash-flow schedule expansion](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/72) | UX catalog nền ở Wave 2 đủ rõ | PO duyệt các flow chi tiết, lịch và lịch sử định giá |
| Backend | [#50 Personal profile & exchange-rate settings](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/50)<br>[#53 Asset catalog & common CRUD](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/53)<br>[#71 Cash-flow entry catalog & CRUD](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/71)<br>[#86 Generic financial goals & templates](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/86) | #42 và #45 hoàn tất | Bốn API domain nền có migration, authorization và test |
| Frontend | [#49 Personal profile & exchange-rate settings](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/49)<br>[#52 Asset catalog & common CRUD](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/52)<br>[#70 Cash-flow entry catalog & CRUD](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/70)<br>[#85 Generic financial goals & templates](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/85) | Design Wave 2 Approved và API tương ứng ở hàng Backend Ready | CRUD end-to-end của bốn domain nền hoạt động |

### Wave 4 — Chi tiết tài sản, công nợ và recurring schedule

| Team | Task có thể làm song song | Điều kiện bắt đầu | Kết quả/gate cần đạt |
|---|---|---|---|
| Design | [#63 Wealth snapshot dashboard](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/63)<br>[#75 Monthly cash-flow reports & composition](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/75)<br>[#78 Three-month cash-flow forecast](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/78) | Domain models từ Wave 3 ổn định | PO duyệt dashboard, report composition và forecast states |
| Backend | [#56 Type-specific asset details](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/56)<br>[#59 Append-only asset valuation history](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/59)<br>[#62 Liability catalog, schedules & collateral](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/62)<br>[#74 Recurring cash-flow schedule expansion](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/74) | API asset/cash-flow nền ở Wave 3 Ready | API domain chiều sâu có test và dữ liệu mẫu nhất quán |
| Frontend | [#55 Type-specific asset details](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/55)<br>[#58 Append-only asset valuation history](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/58)<br>[#61 Liability catalog, schedules & collateral](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/61)<br>[#73 Recurring cash-flow schedule expansion](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/73) | Design Wave 3 Approved và API tương ứng Ready | Các flow domain chiều sâu hoạt động end-to-end |

### Wave 5 — Dashboard, báo cáo và forecast

| Team | Task có thể làm song song | Điều kiện bắt đầu | Kết quả/gate cần đạt |
|---|---|---|---|
| Design | [#66 Explainable Wealth Snapshot insights](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/66)<br>[#81 Financial-health indicators & insights](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/81)<br>[#87 Transparent goal projection & status](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/87) | Dashboard/report design Wave 4 đủ ổn định | PO duyệt cách giải thích số liệu, indicator và goal status |
| Backend | [#65 Wealth snapshot dashboard](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/65)<br>[#77 Monthly cash-flow reports & composition](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/77)<br>[#80 Three-month cash-flow forecast](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/80) | Các API domain chiều sâu ở Wave 4 Ready | Aggregate/calculation API có deterministic tests |
| Frontend | [#64 Wealth snapshot dashboard](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/64)<br>[#76 Monthly cash-flow reports & composition](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/76)<br>[#79 Three-month cash-flow forecast](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/79) | Design Wave 4 Approved và aggregate API Ready | Dashboard, monthly report và forecast tích hợp hoàn chỉnh |

### Wave 6 — Insights, financial health và goal projection

| Team | Task có thể làm song song | Điều kiện bắt đầu | Kết quả/gate cần đạt |
|---|---|---|---|
| Design | [#90 Basic Advisor rule-based report](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/90)<br>[#93 Guided onboarding & financial-profile checklist](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/93)<br>[#96 Validation, empty, loading & error-state hardening](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/96)<br>[#99 Search & basic filters](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/99)<br>[#102 Safe edit & delete flows](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/102)<br>[#108 In-product feedback & privacy-safe observability](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/108) | Các user journey chính đã nhìn thấy được ở Wave 5 | PO duyệt Advisor và toàn bộ UX pilot-readiness |
| Backend | [#68 Explainable Wealth Snapshot insights](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/68)<br>[#83 Financial-health indicators & insights](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/83)<br>[#89 Transparent goal projection & status](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/89) | Calculation APIs Wave 5 Ready | Rule/explanation/projection APIs có test bằng ví dụ nghiệp vụ |
| Frontend | [#67 Explainable Wealth Snapshot insights](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/67)<br>[#82 Financial-health indicators & insights](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/82)<br>[#88 Transparent goal projection & status](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/88) | Design Wave 5 Approved và API tương ứng Ready | Insight, health và goal projection hiển thị giải thích được |

### Wave 7 — Advisor và backend pilot-readiness

| Team | Task có thể làm song song | Điều kiện bắt đầu | Kết quả/gate cần đạt |
|---|---|---|---|
| Design | [#105 Responsive, accessibility & interaction polish](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/105)<br>[#111 Printable dashboard/PDF summary feasibility & delivery](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/111) | Các màn hình chính và Advisor design đã được duyệt | PO duyệt final responsive/a11y và phương án printable/PDF |
| Backend | [#92 Basic Advisor rule-based report](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/92)<br>[#95 Guided onboarding & financial-profile checklist](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/95)<br>[#98 Validation, empty, loading & error-state hardening](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/98)<br>[#101 Search & basic filters](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/101)<br>[#104 Safe edit & delete flows](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/104)<br>[#110 In-product feedback & privacy-safe observability](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/110) | Wave 6 APIs hoàn tất; các domain CRUD ổn định | Advisor và backend pilot-readiness có test/security/privacy checks |
| Frontend | [#91 Basic Advisor rule-based report](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/91) | #90 Design Approved; #92 API/fixtures Ready trong wave | Advisor report hoạt động end-to-end |

### Wave 8 — Frontend pilot-readiness và export backend

| Team | Task có thể làm song song | Điều kiện bắt đầu | Kết quả/gate cần đạt |
|---|---|---|---|
| Design | Review chéo và xử lý feedback PO; không mở thêm scope mới | Các design task đã Approved | Không còn design blocker mức P0/P1 |
| Backend | [#107 Responsive, accessibility & interaction polish](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/107)<br>[#113 Printable dashboard/PDF summary feasibility & delivery](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/113) | Advisor/dashboard/report hoàn tất; #111 chốt hướng export | Backend support cho a11y/performance và export/PDF sẵn sàng |
| Frontend | [#94 Guided onboarding & financial-profile checklist](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/94)<br>[#97 Validation, empty, loading & error-state hardening](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/97)<br>[#100 Search & basic filters](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/100)<br>[#103 Safe edit & delete flows](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/103)<br>[#109 In-product feedback & privacy-safe observability](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/109) | Design Wave 6 Approved; backend tương ứng Wave 7 Ready | Các flow pilot-readiness hoàn tất và qua integration tests |

### Wave 9 — Final polish, printable/PDF và pilot release

| Team | Task có thể làm song song | Điều kiện bắt đầu | Kết quả/gate cần đạt |
|---|---|---|---|
| Design | UAT cùng PO và sign-off giao diện pilot | Wave 8 hoàn tất | PO chấp thuận release candidate |
| Backend | Fix lỗi integration/UAT thuộc phạm vi ticket; không mở feature mới | Wave 8 hoàn tất | Không còn lỗi P0/P1; CI và preview ổn định |
| Frontend | [#106 Responsive, accessibility & interaction polish](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/106)<br>[#112 Printable dashboard/PDF summary feasibility & delivery](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/112) | #105/#111 Design Approved; #107/#113 Backend Ready | A11y/responsive/export qua UAT và pilot release gate |

### Hàng đợi giao việc theo team

Đây là thứ tự ưu tiên để team lead kéo task tiếp theo. Các task trong cùng một nhóm ngoặc có thể chạy song song; dấu `→` là gate bắt buộc trước khi chuyển nhóm.

| Team | Thứ tự ưu tiên |
|---|---|
| Design | `#36` → (`#40`, `#43`) → (`#48`, `#51`, `#69`, `#84`) → (`#54`, `#57`, `#60`, `#72`) → (`#63`, `#75`, `#78`) → (`#66`, `#81`, `#87`) → (`#90`, `#93`, `#96`, `#99`, `#102`, `#108`) → (`#105`, `#111`) → PO/UAT sign-off |
| Backend | `#35` → (`#39`, `#47`) → (`#42`, `#45`) → (`#50`, `#53`, `#71`, `#86`) → (`#56`, `#59`, `#62`, `#74`) → (`#65`, `#77`, `#80`) → (`#68`, `#83`, `#89`) → (`#92`, `#95`, `#98`, `#101`, `#104`, `#110`) → (`#107`, `#113`) → integration/UAT fixes |
| Frontend | `#34` → (`#37`, `#46`) → (`#38`, `#41`, `#44`) → (`#49`, `#52`, `#70`, `#85`) → (`#55`, `#58`, `#61`, `#73`) → (`#64`, `#76`, `#79`) → (`#67`, `#82`, `#88`) → `#91` → (`#94`, `#97`, `#100`, `#103`, `#109`) → (`#106`, `#112`) |

### Quy tắc vận hành mỗi Feature

1. Design và Backend nhận task cùng Feature trong wave tương ứng và làm song song.
2. Design gắn bằng chứng prototype/spec vào Issue và chuyển sang **PO Review**; PO duyệt mới đạt **Design Approved**.
3. Backend gắn OpenAPI/schema, migration notes, fixtures và test evidence; contract ổn định mới đạt **API Ready**.
4. Frontend chỉ bắt đầu khi cả Design Approved và API Ready, ngoại trừ các task nền tảng được ghi rõ ở Wave 0–2.
5. Frontend tích hợp bằng typed client; không tự tạo contract khác với Backend để unblock tạm thời.
6. Feature coordinator chạy acceptance criteria end-to-end, kiểm tra responsive/error states và CI trước khi đóng Feature.

## Backlog chi tiết

### [MVP 0 — Foundation](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/1)

Epic effort: **56 engineer-days** · Status: **Todo**

| Cấp | Ticket | Nhóm | Estimate | Roll-up | Trạng thái | Phụ thuộc |
|---|---|---|---:|---:|---|---|
| Feature | [#6 Repository & local development foundation](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/6) | Cross-team | — | **8d** | In Progress | — |
| ↳ Task | [#34 Repository & local development foundation](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/34) | Frontend | M / 3d | 3d | Todo | Parent: [#6](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/6) |
| ↳ Task | [#35 Repository & local development foundation](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/35) | Backend | L / 5d | 5d | Todo | Parent: [#6](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/6) |
| Feature | [#7 Design system & responsive application shell](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/7) | Cross-team | — | **8d** | Todo | — |
| ↳ Task | [#36 Design system & responsive application shell](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/36) | Design | M / 3d | 3d | Todo | Parent: [#7](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/7) |
| ↳ Task | [#37 Design system & responsive application shell](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/37) | Frontend | L / 5d | 5d | Todo | Parent: [#7](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/7) |
| Feature | [#8 API contract, errors & typed client foundation](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/8) | Cross-team | — | **8d** | Todo | [#6](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/6) Repository & local development foundation |
| ↳ Task | [#38 API contract, errors & typed client foundation](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/38) | Frontend | M / 3d | 3d | Todo | Parent: [#8](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/8) |
| ↳ Task | [#39 API contract, errors & typed client foundation](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/39) | Backend | L / 5d | 5d | Todo | Parent: [#8](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/8) |
| Feature | [#9 Identity, session & ownership boundary](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/9) | Cross-team | — | **12d** | Todo | [#6](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/6) Repository & local development foundation<br>[#8](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/8) API contract, errors & typed client foundation |
| ↳ Task | [#40 Identity, session & ownership boundary](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/40) | Design | S / 2d | 2d | Todo | Parent: [#9](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/9) |
| ↳ Task | [#41 Identity, session & ownership boundary](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/41) | Frontend | L / 5d | 5d | Todo | Parent: [#9](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/9) |
| ↳ Task | [#42 Identity, session & ownership boundary](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/42) | Backend | L / 5d | 5d | Todo | Parent: [#9](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/9) |
| Feature | [#10 Money, currency, exchange-rate & time primitives](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/10) | Cross-team | — | **10d** | Todo | [#6](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/6) Repository & local development foundation<br>[#8](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/8) API contract, errors & typed client foundation |
| ↳ Task | [#43 Money, currency, exchange-rate & time primitives](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/43) | Design | S / 2d | 2d | Todo | Parent: [#10](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/10) |
| ↳ Task | [#44 Money, currency, exchange-rate & time primitives](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/44) | Frontend | M / 3d | 3d | Todo | Parent: [#10](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/10) |
| ↳ Task | [#45 Money, currency, exchange-rate & time primitives](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/45) | Backend | L / 5d | 5d | Todo | Parent: [#10](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/10) |
| Feature | [#11 CI/CD quality gates & AWS preview baseline](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/11) | Cross-team | — | **10d** | Todo | [#6](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/6) Repository & local development foundation |
| ↳ Task | [#46 CI/CD quality gates & AWS preview baseline](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/46) | Frontend | L / 5d | 5d | Todo | Parent: [#11](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/11) |
| ↳ Task | [#47 CI/CD quality gates & AWS preview baseline](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/47) | Backend | L / 5d | 5d | Todo | Parent: [#11](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/11) |

### [MVP 1A — Wealth Snapshot](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/2)

Epic effort: **75 engineer-days** · Status: **Todo**

| Cấp | Ticket | Nhóm | Estimate | Roll-up | Trạng thái | Phụ thuộc |
|---|---|---|---:|---:|---|---|
| Feature | [#12 Personal profile & exchange-rate settings](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/12) | Cross-team | — | **8d** | Todo | [#9](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/9) Identity, session & ownership boundary<br>[#10](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/10) Money, currency, exchange-rate & time primitives<br>[#7](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/7) Design system & responsive application shell |
| ↳ Task | [#48 Personal profile & exchange-rate settings](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/48) | Design | S / 2d | 2d | Todo | Parent: [#12](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/12) |
| ↳ Task | [#49 Personal profile & exchange-rate settings](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/49) | Frontend | M / 3d | 3d | Todo | Parent: [#12](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/12) |
| ↳ Task | [#50 Personal profile & exchange-rate settings](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/50) | Backend | M / 3d | 3d | Todo | Parent: [#12](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/12) |
| Feature | [#13 Asset catalog & common CRUD](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/13) | Cross-team | — | **13d** | Todo | [#9](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/9) Identity, session & ownership boundary<br>[#10](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/10) Money, currency, exchange-rate & time primitives<br>[#7](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/7) Design system & responsive application shell |
| ↳ Task | [#51 Asset catalog & common CRUD](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/51) | Design | M / 3d | 3d | Todo | Parent: [#13](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/13) |
| ↳ Task | [#52 Asset catalog & common CRUD](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/52) | Frontend | L / 5d | 5d | Todo | Parent: [#13](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/13) |
| ↳ Task | [#53 Asset catalog & common CRUD](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/53) | Backend | L / 5d | 5d | Todo | Parent: [#13](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/13) |
| Feature | [#14 Type-specific asset details](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/14) | Cross-team | — | **8d** | Todo | [#13](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/13) Asset catalog & common CRUD |
| ↳ Task | [#54 Type-specific asset details](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/54) | Design | S / 2d | 2d | Todo | Parent: [#14](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/14) |
| ↳ Task | [#55 Type-specific asset details](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/55) | Frontend | M / 3d | 3d | Todo | Parent: [#14](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/14) |
| ↳ Task | [#56 Type-specific asset details](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/56) | Backend | M / 3d | 3d | Todo | Parent: [#14](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/14) |
| Feature | [#15 Append-only asset valuation history](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/15) | Cross-team | — | **10d** | Todo | [#13](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/13) Asset catalog & common CRUD<br>[#10](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/10) Money, currency, exchange-rate & time primitives |
| ↳ Task | [#57 Append-only asset valuation history](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/57) | Design | S / 2d | 2d | Todo | Parent: [#15](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/15) |
| ↳ Task | [#58 Append-only asset valuation history](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/58) | Frontend | M / 3d | 3d | Todo | Parent: [#15](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/15) |
| ↳ Task | [#59 Append-only asset valuation history](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/59) | Backend | L / 5d | 5d | Todo | Parent: [#15](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/15) |
| Feature | [#16 Liability catalog, schedules & collateral](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/16) | Cross-team | — | **13d** | Todo | [#9](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/9) Identity, session & ownership boundary<br>[#10](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/10) Money, currency, exchange-rate & time primitives<br>[#13](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/13) Asset catalog & common CRUD |
| ↳ Task | [#60 Liability catalog, schedules & collateral](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/60) | Design | M / 3d | 3d | Todo | Parent: [#16](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/16) |
| ↳ Task | [#61 Liability catalog, schedules & collateral](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/61) | Frontend | L / 5d | 5d | Todo | Parent: [#16](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/16) |
| ↳ Task | [#62 Liability catalog, schedules & collateral](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/62) | Backend | L / 5d | 5d | Todo | Parent: [#16](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/16) |
| Feature | [#17 Wealth snapshot dashboard](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/17) | Cross-team | — | **13d** | Todo | [#12](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/12) Personal profile & exchange-rate settings<br>[#13](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/13) Asset catalog & common CRUD<br>[#15](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/15) Append-only asset valuation history<br>[#16](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/16) Liability catalog, schedules & collateral<br>[#7](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/7) Design system & responsive application shell |
| ↳ Task | [#63 Wealth snapshot dashboard](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/63) | Design | M / 3d | 3d | Todo | Parent: [#17](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/17) |
| ↳ Task | [#64 Wealth snapshot dashboard](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/64) | Frontend | L / 5d | 5d | Todo | Parent: [#17](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/17) |
| ↳ Task | [#65 Wealth snapshot dashboard](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/65) | Backend | L / 5d | 5d | Todo | Parent: [#17](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/17) |
| Feature | [#18 Explainable Wealth Snapshot insights](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/18) | Cross-team | — | **10d** | Todo | [#17](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/17) Wealth snapshot dashboard<br>[#8](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/8) API contract, errors & typed client foundation |
| ↳ Task | [#66 Explainable Wealth Snapshot insights](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/66) | Design | S / 2d | 2d | Todo | Parent: [#18](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/18) |
| ↳ Task | [#67 Explainable Wealth Snapshot insights](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/67) | Frontend | M / 3d | 3d | Todo | Parent: [#18](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/18) |
| ↳ Task | [#68 Explainable Wealth Snapshot insights](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/68) | Backend | L / 5d | 5d | Todo | Parent: [#18](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/18) |

### [MVP 1B — Cash Flow & Financial Health](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/3)

Epic effort: **62 engineer-days** · Status: **Todo**

| Cấp | Ticket | Nhóm | Estimate | Roll-up | Trạng thái | Phụ thuộc |
|---|---|---|---:|---:|---|---|
| Feature | [#19 Cash-flow entry catalog & CRUD](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/19) | Cross-team | — | **13d** | Todo | [#9](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/9) Identity, session & ownership boundary<br>[#10](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/10) Money, currency, exchange-rate & time primitives<br>[#7](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/7) Design system & responsive application shell |
| ↳ Task | [#69 Cash-flow entry catalog & CRUD](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/69) | Design | M / 3d | 3d | Todo | Parent: [#19](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/19) |
| ↳ Task | [#70 Cash-flow entry catalog & CRUD](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/70) | Frontend | L / 5d | 5d | Todo | Parent: [#19](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/19) |
| ↳ Task | [#71 Cash-flow entry catalog & CRUD](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/71) | Backend | L / 5d | 5d | Todo | Parent: [#19](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/19) |
| Feature | [#20 Recurring cash-flow schedule expansion](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/20) | Cross-team | — | **10d** | Todo | [#19](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/19) Cash-flow entry catalog & CRUD |
| ↳ Task | [#72 Recurring cash-flow schedule expansion](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/72) | Design | S / 2d | 2d | Todo | Parent: [#20](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/20) |
| ↳ Task | [#73 Recurring cash-flow schedule expansion](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/73) | Frontend | M / 3d | 3d | Todo | Parent: [#20](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/20) |
| ↳ Task | [#74 Recurring cash-flow schedule expansion](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/74) | Backend | L / 5d | 5d | Todo | Parent: [#20](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/20) |
| Feature | [#21 Monthly cash-flow reports & composition](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/21) | Cross-team | — | **13d** | Todo | [#19](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/19) Cash-flow entry catalog & CRUD<br>[#20](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/20) Recurring cash-flow schedule expansion |
| ↳ Task | [#75 Monthly cash-flow reports & composition](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/75) | Design | M / 3d | 3d | Todo | Parent: [#21](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/21) |
| ↳ Task | [#76 Monthly cash-flow reports & composition](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/76) | Frontend | L / 5d | 5d | Todo | Parent: [#21](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/21) |
| ↳ Task | [#77 Monthly cash-flow reports & composition](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/77) | Backend | L / 5d | 5d | Todo | Parent: [#21](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/21) |
| Feature | [#22 Three-month cash-flow forecast](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/22) | Cross-team | — | **13d** | Todo | [#20](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/20) Recurring cash-flow schedule expansion<br>[#16](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/16) Liability catalog, schedules & collateral |
| ↳ Task | [#78 Three-month cash-flow forecast](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/78) | Design | M / 3d | 3d | Todo | Parent: [#22](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/22) |
| ↳ Task | [#79 Three-month cash-flow forecast](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/79) | Frontend | L / 5d | 5d | Todo | Parent: [#22](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/22) |
| ↳ Task | [#80 Three-month cash-flow forecast](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/80) | Backend | L / 5d | 5d | Todo | Parent: [#22](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/22) |
| Feature | [#23 Financial-health indicators & insights](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/23) | Cross-team | — | **13d** | Todo | [#21](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/21) Monthly cash-flow reports & composition<br>[#22](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/22) Three-month cash-flow forecast<br>[#17](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/17) Wealth snapshot dashboard<br>[#18](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/18) Explainable Wealth Snapshot insights |
| ↳ Task | [#81 Financial-health indicators & insights](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/81) | Design | M / 3d | 3d | Todo | Parent: [#23](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/23) |
| ↳ Task | [#82 Financial-health indicators & insights](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/82) | Frontend | L / 5d | 5d | Todo | Parent: [#23](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/23) |
| ↳ Task | [#83 Financial-health indicators & insights](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/83) | Backend | L / 5d | 5d | Todo | Parent: [#23](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/23) |

### [MVP 1C — Goals & Basic Advisor](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/4)

Epic effort: **37 engineer-days** · Status: **Todo**

| Cấp | Ticket | Nhóm | Estimate | Roll-up | Trạng thái | Phụ thuộc |
|---|---|---|---:|---:|---|---|
| Feature | [#24 Generic financial goals & templates](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/24) | Cross-team | — | **11d** | Todo | [#9](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/9) Identity, session & ownership boundary<br>[#10](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/10) Money, currency, exchange-rate & time primitives<br>[#7](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/7) Design system & responsive application shell |
| ↳ Task | [#84 Generic financial goals & templates](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/84) | Design | M / 3d | 3d | Todo | Parent: [#24](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/24) |
| ↳ Task | [#85 Generic financial goals & templates](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/85) | Frontend | L / 5d | 5d | Todo | Parent: [#24](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/24) |
| ↳ Task | [#86 Generic financial goals & templates](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/86) | Backend | M / 3d | 3d | Todo | Parent: [#24](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/24) |
| Feature | [#25 Transparent goal projection & status](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/25) | Cross-team | — | **13d** | Todo | [#24](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/24) Generic financial goals & templates<br>[#21](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/21) Monthly cash-flow reports & composition |
| ↳ Task | [#87 Transparent goal projection & status](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/87) | Design | M / 3d | 3d | Todo | Parent: [#25](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/25) |
| ↳ Task | [#88 Transparent goal projection & status](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/88) | Frontend | L / 5d | 5d | Todo | Parent: [#25](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/25) |
| ↳ Task | [#89 Transparent goal projection & status](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/89) | Backend | L / 5d | 5d | Todo | Parent: [#25](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/25) |
| Feature | [#26 Basic Advisor rule-based report](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/26) | Cross-team | — | **13d** | Todo | [#25](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/25) Transparent goal projection & status<br>[#23](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/23) Financial-health indicators & insights<br>[#18](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/18) Explainable Wealth Snapshot insights |
| ↳ Task | [#90 Basic Advisor rule-based report](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/90) | Design | M / 3d | 3d | Todo | Parent: [#26](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/26) |
| ↳ Task | [#91 Basic Advisor rule-based report](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/91) | Frontend | L / 5d | 5d | Todo | Parent: [#26](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/26) |
| ↳ Task | [#92 Basic Advisor rule-based report](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/92) | Backend | L / 5d | 5d | Todo | Parent: [#26](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/26) |

### [MVP 1D — Pilot Readiness](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/5)

Epic effort: **77 engineer-days** · Status: **Todo**

| Cấp | Ticket | Nhóm | Estimate | Roll-up | Trạng thái | Phụ thuộc |
|---|---|---|---:|---:|---|---|
| Feature | [#27 Guided onboarding & financial-profile checklist](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/27) | Cross-team | — | **11d** | Todo | [#12](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/12) Personal profile & exchange-rate settings<br>[#13](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/13) Asset catalog & common CRUD<br>[#16](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/16) Liability catalog, schedules & collateral<br>[#19](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/19) Cash-flow entry catalog & CRUD<br>[#24](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/24) Generic financial goals & templates |
| ↳ Task | [#93 Guided onboarding & financial-profile checklist](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/93) | Design | M / 3d | 3d | Todo | Parent: [#27](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/27) |
| ↳ Task | [#94 Guided onboarding & financial-profile checklist](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/94) | Frontend | L / 5d | 5d | Todo | Parent: [#27](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/27) |
| ↳ Task | [#95 Guided onboarding & financial-profile checklist](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/95) | Backend | M / 3d | 3d | Todo | Parent: [#27](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/27) |
| Feature | [#28 Validation, empty, loading & error-state hardening](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/28) | Cross-team | — | **13d** | Todo | [#8](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/8) API contract, errors & typed client foundation<br>[#7](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/7) Design system & responsive application shell |
| ↳ Task | [#96 Validation, empty, loading & error-state hardening](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/96) | Design | M / 3d | 3d | Todo | Parent: [#28](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/28) |
| ↳ Task | [#97 Validation, empty, loading & error-state hardening](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/97) | Frontend | L / 5d | 5d | Todo | Parent: [#28](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/28) |
| ↳ Task | [#98 Validation, empty, loading & error-state hardening](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/98) | Backend | L / 5d | 5d | Todo | Parent: [#28](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/28) |
| Feature | [#29 Search & basic filters](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/29) | Cross-team | — | **10d** | Todo | [#13](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/13) Asset catalog & common CRUD<br>[#16](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/16) Liability catalog, schedules & collateral<br>[#19](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/19) Cash-flow entry catalog & CRUD<br>[#24](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/24) Generic financial goals & templates |
| ↳ Task | [#99 Search & basic filters](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/99) | Design | S / 2d | 2d | Todo | Parent: [#29](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/29) |
| ↳ Task | [#100 Search & basic filters](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/100) | Frontend | L / 5d | 5d | Todo | Parent: [#29](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/29) |
| ↳ Task | [#101 Search & basic filters](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/101) | Backend | M / 3d | 3d | Todo | Parent: [#29](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/29) |
| Feature | [#30 Safe edit & delete flows](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/30) | Cross-team | — | **13d** | Todo | [#13](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/13) Asset catalog & common CRUD<br>[#16](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/16) Liability catalog, schedules & collateral<br>[#19](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/19) Cash-flow entry catalog & CRUD<br>[#24](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/24) Generic financial goals & templates<br>[#7](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/7) Design system & responsive application shell |
| ↳ Task | [#102 Safe edit & delete flows](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/102) | Design | M / 3d | 3d | Todo | Parent: [#30](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/30) |
| ↳ Task | [#103 Safe edit & delete flows](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/103) | Frontend | L / 5d | 5d | Todo | Parent: [#30](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/30) |
| ↳ Task | [#104 Safe edit & delete flows](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/104) | Backend | L / 5d | 5d | Todo | Parent: [#30](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/30) |
| Feature | [#31 Responsive, accessibility & interaction polish](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/31) | Cross-team | — | **12d** | Todo | [#7](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/7) Design system & responsive application shell |
| ↳ Task | [#105 Responsive, accessibility & interaction polish](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/105) | Design | L / 5d | 5d | Todo | Parent: [#31](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/31) |
| ↳ Task | [#106 Responsive, accessibility & interaction polish](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/106) | Frontend | L / 5d | 5d | Todo | Parent: [#31](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/31) |
| ↳ Task | [#107 Responsive, accessibility & interaction polish](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/107) | Backend | S / 2d | 2d | Todo | Parent: [#31](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/31) |
| Feature | [#32 In-product feedback & privacy-safe observability](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/32) | Cross-team | — | **11d** | Todo | [#9](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/9) Identity, session & ownership boundary<br>[#8](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/8) API contract, errors & typed client foundation<br>[#7](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/7) Design system & responsive application shell |
| ↳ Task | [#108 In-product feedback & privacy-safe observability](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/108) | Design | M / 3d | 3d | Todo | Parent: [#32](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/32) |
| ↳ Task | [#109 In-product feedback & privacy-safe observability](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/109) | Frontend | M / 3d | 3d | Todo | Parent: [#32](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/32) |
| ↳ Task | [#110 In-product feedback & privacy-safe observability](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/110) | Backend | L / 5d | 5d | Todo | Parent: [#32](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/32) |
| Feature | [#33 Printable dashboard/PDF summary feasibility & delivery](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/33) | Cross-team | — | **7d** | Todo | [#17](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/17) Wealth snapshot dashboard<br>[#21](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/21) Monthly cash-flow reports & composition<br>[#26](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/26) Basic Advisor rule-based report |
| ↳ Task | [#111 Printable dashboard/PDF summary feasibility & delivery](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/111) | Design | S / 2d | 2d | Todo | Parent: [#33](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/33) |
| ↳ Task | [#112 Printable dashboard/PDF summary feasibility & delivery](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/112) | Frontend | M / 3d | 3d | Todo | Parent: [#33](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/33) |
| ↳ Task | [#113 Printable dashboard/PDF summary feasibility & delivery](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/113) | Backend | S / 2d | 2d | Todo | Parent: [#33](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS/issues/33) |

## Chiến lược dùng Codex session

### Mặc định: một session thực thi cho một Task

Mỗi Task trong bảng là đơn vị phù hợp nhất để giao cho một Codex session: một discipline chính, effort 1–5 ngày, phạm vi file/module rõ và một PR có thể review độc lập. Feature nên có một session điều phối/tích hợp để giữ contract, theo dõi dependency, chạy kiểm thử end-to-end và đóng Feature sau khi các Task con hoàn tất.

### Khi nào có thể giao cả Feature cho một session

Chỉ nên làm khi Feature có tối đa ba Task con, tổng effort khoảng 5–8 ngày, contract đã ổn định, không thay đổi auth/schema/tính toán tiền dùng chung, và acceptance criteria có thể kiểm chứng end-to-end trong một môi trường. Với Feature lớn hơn, session vẫn có thể điều phối nhiều sub-agent, nhưng mỗi sub-agent phải có ownership tách biệt và không cùng sửa một file/config/migration.

### Guardrails chống hallucination

- Luôn đưa link Issue, tài liệu nguồn, working directory, outcome, non-goals, acceptance criteria và dependency vào prompt.
- Bắt đầu bằng việc đọc repository và xác minh contract hiện có; không cho phép tự phát minh API, schema hoặc UX khi tài liệu chưa quyết định.
- Một worktree/branch cho mỗi Task session; một owner rõ cho từng file/module; một PR cho một outcome.
- Backend công bố OpenAPI/schema/fixtures ổn định trước khi Frontend tích hợp; Design công bố states và responsive behavior trước khi UI được coi là hoàn tất.
- Session phải chạy test/lint/typecheck phù hợp và trích bằng chứng trước khi tuyên bố Done. Feature chỉ đóng sau integration review và CI.
- Dùng sub-agent có vai trò rõ: explorer chỉ đọc/đối chiếu; implementer sở hữu vùng code; tester/reviewer kiểm chứng độc lập. Không cho nhiều agent cùng sửa shared config hoặc migration.

## Cách đọc ticket

Nội dung Who / When / What / How và acceptance criteria nằm trong từng GitHub Issue được liên kết ở bảng trên. Tài liệu nguồn chuẩn là [MVP Backbone](./RIKKAUS_WEALTH_OS_MVP_BACKBONE.md) và [Architecture & Technology Decisions](./ARCHITECTURE_TECHNOLOGY_DECISIONS.md).

## Nguồn quản lý

- [GitHub Project #6](https://github.com/users/thientrinhcoder/projects/6)
- [GitHub repository](https://github.com/thientrinhcoder/RIKKAUS_WEALTH_OS)
- Snapshot này là tài liệu tổng quan; trạng thái và quan hệ issue trên GitHub Project là nguồn vận hành trực tiếp.
