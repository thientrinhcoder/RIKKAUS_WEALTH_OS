# Phase 2 — Service và route forecast

Status: Pending · Depends on: phase 1

## Files (tạo mới, chỉ trong `cashflow.forecast`)

- `services/api/src/main/java/com/rikkaus/wealth/cashflow/forecast/application/ForecastHorizon.java`:
  parse `from` (`YYYY-MM`), mặc định tháng kế tiếp, biên D3, lỗi `validation-failed` field `from`.
- `.../forecast/application/ForecastSourceReader.java`: `JdbcClient`, ba truy vấn native có lọc `user_id`
  (tỷ giá, tài sản `CASH`/`BANK_ACCOUNT`, công nợ).
- `.../forecast/application/CashFlowForecastService.java`: ghép kỳ entry (qua
  `CashFlowOccurrenceService`), kỳ công nợ, khử trùng lặp, quy đổi, số dư, tín hiệu, nguồn bị loại.
- `.../forecast/application/CashFlowForecast.java` và các record view.
- `.../forecast/api/CashFlowForecastController.java`, `CashFlowForecastResponse.java`.

## Steps

1. Horizon và validation.
2. Đọc nguồn native; mở rộng lịch công nợ theo D6–D7 với cùng quy tắc `plusMonths` từ ngày neo như
   `CashFlowSchedule`.
3. Áp D8–D14, tính tổng tháng, chuỗi số dư theo ngày (D15), ngưỡng và trạng thái (D16), thứ tự (D17).
4. Controller với OpenAPI annotation và ví dụ dữ liệu Việt Nam.

## Validation

`clean verify` xanh; case phase 1 chạy ở phase 4.

## Risk

Đọc native lệch tên cột khi slice khác đổi schema: chỉ đọc cột đã có ràng buộc trong V3, V4, V7.
