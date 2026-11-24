# Phase 3 — Audit, docs và evidence

Status: completed.

## Files

- `audit-lists.sh` (L01–L07) và `audit-errors.sh` (E01–E11) trong thư mục plan này; chạy sau các script
  fixture.
- `docs/api-contract-conventions.md`: mục "Pilot list bounds, error shape and local fixtures —
  implemented" ở cuối file, sau mục insights của #68; bullet pagination trỏ tới số đo.
- `plans/reports/manual-verification-261124-0958-GH-107-a11y-backend-support.md`.

## Kết luận

- Không hành vi nào chặn UI responsive/accessibility, nên không sửa Java, không đổi contract.
- Không cần pagination cho pilot (list lớn nhất 86 KB, dưới 25 ms).
- Phát hiện về validation và schema chuyển #98; thứ tự `sort=name` theo collation là câu hỏi PO.

## Rollback

Xoá `services/api/fixtures/`, `docs/local-fixtures-guide.md` và mục docs mới. Không có migration hay dữ
liệu production.
