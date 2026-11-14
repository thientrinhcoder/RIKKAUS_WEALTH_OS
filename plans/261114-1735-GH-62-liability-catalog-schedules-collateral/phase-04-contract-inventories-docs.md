# Phase 4 — Contract, inventory test, docs

## Files

- `services/api/src/test/java/com/rikkaus/wealth/shared/openapi/OpenApiContractIT.java` — thêm
  `/api/v1/liabilities`, `/api/v1/liabilities/{liabilityId}`
- `services/api/src/test/java/com/rikkaus/wealth/ApplicationContextIT.java` — thêm `"liabilities"`
  vào `containsExactly` (thứ tự alphabet: `assets, liabilities, refresh_tokens, user_settings, users`)
- `services/api/openapi/openapi.json` — regenerate bằng
  `JAVA_HOME=... ./services/api/mvnw -f services/api/pom.xml verify -Dopenapi.update=true`, rồi chạy
  lại `verify` **không** flag để chứng minh khớp
- `docs/api-contract-conventions.md` — mục "Liability catalog — implemented": 5 route, 6 category, 4
  tần suất, bảng field required/optional, quy tắc ngày, collateral (validation-failed, SET NULL), PUT
  thay thế trừ category, không chuyển đổi tỷ giá

## Steps

1. Sửa hai inventory test.
2. Chạy `verify -Dopenapi.update=true`, đọc diff `openapi.json` (chỉ thêm path/schema liabilities).
3. Chạy `verify` không flag: phải xanh.
4. Cập nhật conventions doc.

## Validation

`verify` xanh không update flag là gate của phase này và điều kiện để commit.
