# Phase 2 — Service, properties và route

Status: Complete

## Files

- Tạo `services/api/src/main/java/com/rikkaus/wealth/goals/projection/application/GoalProjectionProperties.java`:
  record `@ConfigurationProperties("rikkaus.goals.projection")`, từ chối thiếu hoặc sai thứ tự ngưỡng (D9).
- Tạo `.../goals/projection/application/GoalProjectionConfiguration.java`: `@EnableConfigurationProperties`, cùng lý do
  `IdentityConfiguration` nêu (không bind trong `@WebMvcTest`).
- Tạo `.../goals/projection/application/GoalProjection.java`: kết quả tính, bất biến.
- Tạo `.../goals/projection/application/GoalProjectionCalculator.java`: hàm thuần D3–D13.
- Tạo `.../goals/projection/application/GoalProjectionService.java`: đọc goal qua `GoalService.read` / `listFor`,
  ngày tham chiếu từ `Clock` theo `Asia/Ho_Chi_Minh`.
- Tạo `.../goals/projection/api/GoalProjectionController.java` và `GoalProjectionResponse.java`.
- Sửa `services/api/src/main/resources/application.yml`: khối `rikkaus.goals.projection` với 100 và 80.

## Steps

1. Properties và configuration.
2. Calculator theo D3–D13; `MathContext.DECIMAL128`; không `double`.
3. Service và controller; ví dụ OpenAPI dùng dòng F2 của phase 1.
4. `clean verify` (cần `-Dopenapi.update=true` lần đầu, xem phase 3).

## Validation

Chỉ chạy được sau phase 3 vì `OpenApiContractIT` khóa inventory path; case phase 1 chạy ở phase 4.

## Risk

`GoalService` đang được #104 sửa; chỉ gọi `read` và `listFor` (không đổi chữ ký trong #104), nên xung đột merge
chỉ có thể ở `openapi.json`, regenerate là xong.
