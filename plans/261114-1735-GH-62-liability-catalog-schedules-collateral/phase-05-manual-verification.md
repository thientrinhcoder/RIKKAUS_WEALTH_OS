# Phase 5 — Chạy scenario thật và ghi evidence

## Môi trường

- PostgreSQL dùng một lần: container `rikkaus-pg-issue-62`, `127.0.0.1:5462`, image
  `postgres:18.6-alpine3.24`; không chạm volume `rikkaus-postgres-data`.
- API: jar đã package, `--spring.profiles.active=local --server.port=8062`,
  `DB_URL=jdbc:postgresql://localhost:5462/rikkaus`, JDK 25.
- Hai user A/B insert trực tiếp vào `wealth.users`; token HS256 mint bằng secret dev của profile `local`
  theo `docs/manual-test-guide-assets.md` (đi qua `BearerTokenAuthenticationFilter` thật).
- Mỗi user tạo một asset `REAL_ESTATE` qua `POST /assets` để làm collateral.

## Files

- `docs/manual-test-guide-liabilities.md` — mọi case phase 1 là lệnh curl hoàn chỉnh + kết quả mong đợi
- `plans/reports/manual-verification-261114-HHMM-GH-62-liability-catalog-schedules-collateral.md` —
  status/body thật từng case, commit đang chạy, số case pass/fail; case fail rồi fix ghi là đã fail

## Steps

1. Viết script chạy toàn bộ case theo thứ tự phase 1, in `case | status | body`.
2. Chạy trên commit cuối của code; lưu output thô vào scratchpad, trích vào report.
3. Nếu có case fail: ghi vào report, fix, chạy lại **toàn bộ** danh sách, ghi lượt hai.
4. Tear down: tắt API, `docker rm -f rikkaus-pg-issue-62`.

## Validation

Report ghi rõ `N/N` pass trên commit `<sha>`; mọi `500` là defect phải fix trước khi mở PR.
