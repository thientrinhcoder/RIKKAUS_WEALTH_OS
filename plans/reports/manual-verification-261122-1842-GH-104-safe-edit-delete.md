# Manual verification — issue #104 safe edit & delete flows

**Kết quả: 45/45 scenario PASS trên commit `c8dbeab`** (lần chạy 3, database sạch), và lại 45/45 trên
merge `origin/main` `0eb1ec3` vào nhánh (commit `a13a7d6`, lần chạy 4). Hai lần chạy trước
có một case fail và hai case pass không đáng tin, đều do lỗi của script chấm, không phải của code; chi
tiết ở *Các lần chạy*. `clean verify` không flag xanh trên `c8dbeab`: 131 test (unit + integration),
0 fail, gồm drift check `openapi.json`, `OpenApiContractIT` đã mở rộng và `ArchitectureRulesTest`.

## Môi trường

| Mục | Giá trị |
|---|---|
| Ngày chạy | 22/11/2026, giờ `Asia/Saigon` |
| Database | Container `rikkaus-pg-issue-104`, `postgres:18.6-alpine3.24` (PostgreSQL 18.6 aarch64), `127.0.0.1:5404`, tạo mới cho mỗi lần chạy |
| API | Jar đóng gói bởi `clean verify` không flag trên `c8dbeab`, `--spring.profiles.active=local --server.port=8104`, `DB_URL=jdbc:postgresql://localhost:5404/rikkaus`; Flyway `Successfully applied 11 migrations to schema "public", now at version v14` |
| Token | Bốn token (A, B, hết hạn, ký bằng secret khác) sinh lúc chạy từ secret của profile `local`, chỉ nằm trong biến môi trường của script; report không chứa token |
| Dữ liệu | Đúng bảng dữ liệu trong `phase-01-test-scenarios.md`, tạo qua API thật |
| Cách chấm | Script bash + `curl` + `jq` + `psql` (`docker exec`). Body so bằng `jq -c` (giữ thứ tự key) với chuỗi mong đợi; dữ liệu sau mỗi lần xóa đọc thẳng từ PostgreSQL |

## Các lần chạy

| Lần | Code | Kết quả | Ghi chú |
|---|---|---|---|
| 1 | `c8dbeab` | **44/45**, fail U01 | Lỗi script: bash 3.2 của macOS với `set -u` báo `auth[@]: unbound variable` khi gửi request không token, nên request U01 không được gửi. U04 dùng cùng nhánh code đó và được chấm PASS sai (status rỗng, hàm so sánh lỗi rồi `all` nhận thiếu đối số). Sửa script: mở rộng mảng an toàn và hàm `is` chịu được đối số rỗng |
| 2 | `c8dbeab`, database mới | 45/45 | Khi đọc lại evidence: G06 tìm occurrence theo trường `entryId`, nhưng response dùng `sourceEntryId`, nên "0 occurrence của CF5 sau khi xóa" đúng một cách rỗng. Sửa scenario: đếm trước khi xóa (phải là 1) và sau khi xóa (phải là 0) |
| 3 | `c8dbeab`, database mới | **45/45** | Kết quả ghi bên dưới. U01/U04 gửi thật không token và nhận 401; G06 thấy 1 occurrence của CF5 trước khi xóa, 0 sau khi xóa |
| 4 | Merge `a13a7d6` (`origin/main` `0eb1ec3`: #162 web refresh cookie + dev sign-in local, #163, #164), jar từ `clean verify` không flag, database mới | **45/45** | Merge không conflict; `clean verify -Dopenapi.update=true` không đổi `openapi.json` so với bản git tự gộp; `clean verify` không flag 131 test, 0 fail. Token vẫn mint như cũ vì `AccessTokenService` không đổi, chỉ `SecurityConfiguration` mở thêm `/auth/dev-sign-in` cho profile `local`. C01 `204 404 404 404`, K01 đủ 7 điểm, K02 0/155 dòng log |

## Phát hiện

1. **Không có lỗi code nào lộ ra.** Ba lần chạy đều cho cùng status và body ở mọi case mà script chấm
   đúng; khác biệt chỉ nằm ở hai lỗi chấm nêu trên.
2. **Bốn delete đồng thời có xác nhận** (C01) cho đúng một `204` và ba `404 not-found`; bốn delete không
   xác nhận (C02) cho bốn `409` và không xóa gì. Khóa dòng `PESSIMISTIC_WRITE` trước khi tính impact
   làm request sau chờ request trước rồi thấy dòng đã mất.
3. **`impact` trong 409 bằng đúng body preview** đọc ngay trước đó (G01, G02, G03), kể cả label mới của
   liability sau E01.
4. **Log không chứa tên record hay query string**: 0/155 dòng log của lần 3 khớp `Căn hộ Thảo Điền`,
   `Vay mua nhà` hoặc `confirm=` (K02).

## Evidence lần 3

Status và body thật của từng case, đúng như script ghi lại; id là UUID sinh trong lần chạy này.

## Dữ liệu

| Ký hiệu | id |
|---|---|
| AS1 | `88d9ed07-7f99-43ca-af82-b008a2fce849` |
| AS2 | `663b1f0e-c9c3-4926-bf43-2b401b236971` |
| AS3 | `f4f308b8-351c-4d33-bb74-08bf400f747a` |
| AS4 | `92f254f5-c748-4868-a35e-9ea8abd313a2` |
| AS5 | `12b11661-3b51-4593-b611-b38cb3c1bebf` |
| ASB1 | `d45237f4-ccc2-47a5-8812-1ab9efe77071` |
| LB1 | `95600651-67e4-4eed-843f-a66cfffa5058` |
| LB2 | `42069782-598a-43c7-8cb8-8934cbee2606` |
| LBB1 | `a88e1146-69b2-4f29-ac06-82ccec011d6c` |
| CF1 | `f9564318-fc77-4890-a53c-12f89567ac74` |
| CF2 | `0767fda4-43f4-4a6c-ba30-b98fbb647890` |
| CF3 | `4c52ae22-9fc1-4a36-b3d5-7a3020a97c7f` |
| CF4 | `b30bfaaa-29fb-469b-a344-dbaf8dcb059a` |
| CF5 | `3386c534-4e50-4314-972a-5c62587785d1` |
| CF6 | `c9c18de5-4337-46ef-9071-6b5822622b60` |
| CF7 | `cd720107-b388-4b9f-a0d9-ea50bc649173` |
| CFB1 | `bd72b424-e572-4ad8-8d5f-f04a04b0f1c6` |
| G1 | `c9c2422e-3510-4d5b-893d-937255e8f9ab` |
| G2 | `b60a12e6-10d8-421f-a3d0-c1e2e01dca96` |
| GB1 | `f673d97f-f06d-456b-a0ad-a155614e2501` |

psql: số valuation của AS1 trước khi chạy = 3

## I — Deletion-impact preview
#### I01 — PASS

`GET /assets/AS2/deletion-impact` → **200**

```json
{
  "recordType": "ASSET",
  "recordId": "663b1f0e-c9c3-4926-bf43-2b401b236971",
  "confirmationRequired": false,
  "linkedEffects": [
    {
      "recordType": "ASSET_VALUATION",
      "effect": "DELETED",
      "field": null,
      "count": 1,
      "records": []
    }
  ],
  "derivedEffects": []
}
```

#### I02 — PASS

`GET /assets/AS1/deletion-impact` → **200**

```json
{
  "recordType": "ASSET",
  "recordId": "88d9ed07-7f99-43ca-af82-b008a2fce849",
  "confirmationRequired": true,
  "linkedEffects": [
    {
      "recordType": "ASSET_VALUATION",
      "effect": "DELETED",
      "field": null,
      "count": 3,
      "records": []
    },
    {
      "recordType": "LIABILITY",
      "effect": "UNLINKED",
      "field": "collateralAssetId",
      "count": 1,
      "records": [
        {
          "id": "95600651-67e4-4eed-843f-a66cfffa5058",
          "label": "Vay mua nhà Vietcombank",
          "category": "BANK_LOAN"
        }
      ]
    },
    {
      "recordType": "CASH_FLOW_ENTRY",
      "effect": "UNLINKED",
      "field": "linkedAssetId",
      "count": 2,
      "records": [
        {
          "id": "f9564318-fc77-4890-a53c-12f89567ac74",
          "label": "Cho thuê căn hộ Thảo Điền",
          "category": "RENTAL_INCOME"
        },
        {
          "id": "0767fda4-43f4-4a6c-ba30-b98fbb647890",
          "label": "Phí quản lý căn hộ Thảo Điền",
          "category": "HOUSING"
        }
      ]
    }
  ],
  "derivedEffects": []
}
```

#### I03 — PASS

`GET /liabilities/LB1/deletion-impact` → **200**

```json
{
  "recordType": "LIABILITY",
  "recordId": "95600651-67e4-4eed-843f-a66cfffa5058",
  "confirmationRequired": true,
  "linkedEffects": [
    {
      "recordType": "CASH_FLOW_ENTRY",
      "effect": "UNLINKED",
      "field": "linkedLiabilityId",
      "count": 1,
      "records": [
        {
          "id": "4c52ae22-9fc1-4a36-b3d5-7a3020a97c7f",
          "label": "Trả góp vay mua nhà Vietcombank",
          "category": "DEBT_PRINCIPAL_PAYMENT"
        }
      ]
    }
  ],
  "derivedEffects": []
}
```

#### I04 — PASS

`GET /liabilities/LB2/deletion-impact` → **200**

```json
{
  "recordType": "LIABILITY",
  "recordId": "42069782-598a-43c7-8cb8-8934cbee2606",
  "confirmationRequired": false,
  "linkedEffects": [],
  "derivedEffects": []
}
```

#### I05 — PASS

`GET /cash-flow-entries/CF1/deletion-impact` → **200**

```json
{
  "recordType": "CASH_FLOW_ENTRY",
  "recordId": "f9564318-fc77-4890-a53c-12f89567ac74",
  "confirmationRequired": false,
  "linkedEffects": [],
  "derivedEffects": [
    "CASH_FLOW_OCCURRENCES",
    "MONTHLY_CASH_FLOW_REPORTS",
    "CASH_FLOW_FORECAST"
  ]
}
```

#### I06 — PASS

`GET /cash-flow-entries/CF5/deletion-impact` → **200**

```json
{
  "recordType": "CASH_FLOW_ENTRY",
  "recordId": "3386c534-4e50-4314-972a-5c62587785d1",
  "confirmationRequired": false,
  "linkedEffects": [],
  "derivedEffects": [
    "CASH_FLOW_OCCURRENCES",
    "MONTHLY_CASH_FLOW_REPORTS",
    "CASH_FLOW_FORECAST"
  ]
}
```

#### I07 — PASS

`GET /goals/G1/deletion-impact` → **200**

```json
{
  "recordType": "GOAL",
  "recordId": "c9c2422e-3510-4d5b-893d-937255e8f9ab",
  "confirmationRequired": false,
  "linkedEffects": [],
  "derivedEffects": []
}
```

#### I08 — PASS

`psql sau I01–I07` → **psql**

valuation AS1 = 3; collateral LB1 = 88d9ed07-7f99-43ca-af82-b008a2fce849

```json
(empty)
```

## O — Ownership
#### O01 — PASS

`A: GET /assets/<của B>/deletion-impact vs UUID ngẫu nhiên` → **404**

Body UUID ngẫu nhiên sau khi bỏ instance/correlationId: `{"detail":"Resource not found.","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found"}`; body record của B: `{"detail":"Resource not found.","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found"}`

```json
{
  "detail": "Resource not found.",
  "instance": "/api/v1/assets/d45237f4-ccc2-47a5-8812-1ab9efe77071/deletion-impact",
  "status": 404,
  "title": "Resource not found",
  "type": "urn:rikkaus:problem:not-found",
  "correlationId": "fd6b3a04-482f-4cfa-aebb-c78d55571716"
}
```

#### O02 — PASS

`A: GET /liabilities/<của B>/deletion-impact vs UUID ngẫu nhiên` → **404**

Body UUID ngẫu nhiên sau khi bỏ instance/correlationId: `{"detail":"Resource not found.","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found"}`; body record của B: `{"detail":"Resource not found.","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found"}`

```json
{
  "detail": "Resource not found.",
  "instance": "/api/v1/liabilities/a88e1146-69b2-4f29-ac06-82ccec011d6c/deletion-impact",
  "status": 404,
  "title": "Resource not found",
  "type": "urn:rikkaus:problem:not-found",
  "correlationId": "7b6c330c-c419-4839-a6a3-971689858516"
}
```

#### O03 — PASS

`A: GET /cash-flow-entries/<của B>/deletion-impact vs UUID ngẫu nhiên` → **404**

Body UUID ngẫu nhiên sau khi bỏ instance/correlationId: `{"detail":"Resource not found.","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found"}`; body record của B: `{"detail":"Resource not found.","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found"}`

```json
{
  "detail": "Resource not found.",
  "instance": "/api/v1/cash-flow-entries/bd72b424-e572-4ad8-8d5f-f04a04b0f1c6/deletion-impact",
  "status": 404,
  "title": "Resource not found",
  "type": "urn:rikkaus:problem:not-found",
  "correlationId": "fe69cd30-def8-4815-8c4e-024c60eae300"
}
```

#### O04 — PASS

`A: GET /goals/<của B>/deletion-impact vs UUID ngẫu nhiên` → **404**

Body UUID ngẫu nhiên sau khi bỏ instance/correlationId: `{"detail":"Resource not found.","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found"}`; body record của B: `{"detail":"Resource not found.","status":404,"title":"Resource not found","type":"urn:rikkaus:problem:not-found"}`

```json
{
  "detail": "Resource not found.",
  "instance": "/api/v1/goals/f673d97f-f06d-456b-a0ad-a155614e2501/deletion-impact",
  "status": 404,
  "title": "Resource not found",
  "type": "urn:rikkaus:problem:not-found",
  "correlationId": "5c34fcac-a4de-4f05-bc4d-f3966e4576d4"
}
```

#### O05 — PASS

`A: DELETE /assets/ASB1?confirm=true` → **404**

psql (asset còn | collateral LBB1 | link CFB1): `1|d45237f4-ccc2-47a5-8812-1ab9efe77071|d45237f4-ccc2-47a5-8812-1ab9efe77071`

```json
{
  "detail": "Resource not found.",
  "instance": "/api/v1/assets/d45237f4-ccc2-47a5-8812-1ab9efe77071",
  "status": 404,
  "title": "Resource not found",
  "type": "urn:rikkaus:problem:not-found",
  "correlationId": "338a537e-19e9-42e6-9161-82986613957b"
}
```

#### O06 — PASS

`A: DELETE /liabilities/LBB1` → **404**

```json
{
  "detail": "Resource not found.",
  "instance": "/api/v1/liabilities/a88e1146-69b2-4f29-ac06-82ccec011d6c",
  "status": 404,
  "title": "Resource not found",
  "type": "urn:rikkaus:problem:not-found",
  "correlationId": "0fc51189-b458-4ef7-bccb-5a85e661fdbe"
}
```

## U — Authentication
#### U01 — PASS

`GET /assets/AS1/deletion-impact không token` → **401**

```json
{
  "detail": "Authentication required.",
  "status": 401,
  "title": "Authentication required",
  "type": "urn:rikkaus:problem:unauthorized",
  "correlationId": "87f521ce-44a1-4db8-9909-7cce66421994"
}
```

#### U02 — PASS

`… token hết hạn` → **401**

```json
{
  "detail": "Authentication required.",
  "status": 401,
  "title": "Authentication required",
  "type": "urn:rikkaus:problem:unauthorized",
  "correlationId": "40bcf039-2f88-4a85-a19d-4dcd9e583de7"
}
```

#### U03 — PASS

`… token ký sai secret` → **401**

```json
{
  "detail": "Authentication required.",
  "status": 401,
  "title": "Authentication required",
  "type": "urn:rikkaus:problem:unauthorized",
  "correlationId": "b9226ee4-6ad3-46a6-86f2-17f16ed822f5"
}
```

#### U04 — PASS

`DELETE /assets/AS1?confirm=true không token` → **401**

psql: AS1 còn = 1

```json
{
  "detail": "Authentication required.",
  "status": 401,
  "title": "Authentication required",
  "type": "urn:rikkaus:problem:unauthorized",
  "correlationId": "0fbfd4cd-a54d-4a56-8877-d1c2f2ce6503"
}
```

## M — Method và input
#### M01 — PASS

`POST /assets/AS1/deletion-impact` → **405**

```json
{
  "detail": "Method 'POST' is not supported.",
  "instance": "/api/v1/assets/88d9ed07-7f99-43ca-af82-b008a2fce849/deletion-impact",
  "status": 405,
  "title": "Method not allowed",
  "type": "urn:rikkaus:problem:method-not-allowed",
  "correlationId": "aa793fab-e609-48b3-8bcc-e1678de8dcb9"
}
```

#### M02 — PASS

`DELETE /goals/G1/deletion-impact` → **405**

psql: G1 còn = 1

```json
{
  "detail": "Method 'DELETE' is not supported.",
  "instance": "/api/v1/goals/c9c2422e-3510-4d5b-893d-937255e8f9ab/deletion-impact",
  "status": 405,
  "title": "Method not allowed",
  "type": "urn:rikkaus:problem:method-not-allowed",
  "correlationId": "36443873-ad45-47b7-968b-1d212965c74e"
}
```

#### M03 — PASS

`GET /assets/khong-phai-uuid/deletion-impact` → **400**

```json
{
  "detail": "Malformed request.",
  "instance": "/api/v1/assets/khong-phai-uuid/deletion-impact",
  "status": 400,
  "title": "Malformed request",
  "type": "urn:rikkaus:problem:malformed-request",
  "correlationId": "be1dc0f9-2add-4d17-9af5-efcd017c2721"
}
```

#### M04 — PASS

`GET /liabilities/<UUID ngẫu nhiên>/deletion-impact` → **404**

```json
{
  "detail": "Resource not found.",
  "instance": "/api/v1/liabilities/5b0c3f6e-2a19-4d8b-9e47-0c1d2e3f4a5b/deletion-impact",
  "status": 404,
  "title": "Resource not found",
  "type": "urn:rikkaus:problem:not-found",
  "correlationId": "91889165-e491-47a7-ab81-ecc46f8d6a59"
}
```

#### M05 — PASS

`DELETE /assets/AS1?confirm=co-le` → **400**

psql: AS1 còn = 1

```json
{
  "detail": "Malformed request.",
  "instance": "/api/v1/assets/88d9ed07-7f99-43ca-af82-b008a2fce849",
  "status": 400,
  "title": "Malformed request",
  "type": "urn:rikkaus:problem:malformed-request",
  "correlationId": "43c5cb4a-1e43-4478-998a-f0211ad68ffa"
}
```

## E — Safe edit
#### E01 — PASS

`PUT /liabilities/LB1 gửi lại collateralAssetId, đổi tên` → **200**

Impact AS1 sau PUT liệt kê label: `Vay mua nhà Vietcombank (gói ưu đãi 2026)`

```json
{
  "id": "95600651-67e4-4eed-843f-a66cfffa5058",
  "name": "Vay mua nhà Vietcombank (gói ưu đãi 2026)",
  "category": "BANK_LOAN",
  "outstandingBalance": 2400000000.00,
  "currency": "VND",
  "interestRate": 8.5000,
  "startDate": null,
  "maturityDate": null,
  "recurringPaymentAmount": 28500000.00,
  "recurringPaymentFrequency": "MONTHLY",
  "nextPaymentDate": "2026-12-05",
  "collateralAssetId": "88d9ed07-7f99-43ca-af82-b008a2fce849",
  "notes": "Thế chấp căn hộ Thảo Điền.",
  "createdAt": "2026-11-22T11:56:51.967141Z",
  "updatedAt": "2026-11-22T11:56:53.200478Z"
}
```

#### E02 — PASS

`PUT /cash-flow-entries/CF1 gửi lại linkedAssetId, amount 19.000.000` → **200**

```json
{
  "id": "f9564318-fc77-4890-a53c-12f89567ac74",
  "entryType": "INCOME",
  "category": "RENTAL_INCOME",
  "amount": 19000000.00,
  "currency": "VND",
  "timing": "RECURRING",
  "recurrence": "MONTHLY",
  "effectiveDate": "2026-01-01",
  "endDate": null,
  "linkedAssetId": "88d9ed07-7f99-43ca-af82-b008a2fce849",
  "linkedLiabilityId": null,
  "notes": "Cho thuê căn hộ Thảo Điền",
  "createdAt": "2026-11-22T11:56:52.044924Z",
  "updatedAt": "2026-11-22T11:56:53.251330Z"
}
```

#### E03 — PASS

`PUT /assets/AS1 đổi tên` → **200**

psql (collateral LB1 | số cash-flow link AS1): `88d9ed07-7f99-43ca-af82-b008a2fce849|2`; impact counts sau PUT: `[3,1,2]`

```json
{
  "id": "88d9ed07-7f99-43ca-af82-b008a2fce849",
  "name": "Căn hộ Thảo Điền (Masteri)",
  "category": "REAL_ESTATE",
  "currentValue": 4200000000.00,
  "currency": "VND",
  "valuationDate": "2026-11-15",
  "liquidity": "LOW",
  "purchaseValue": null,
  "purchaseDate": null,
  "quantity": null,
  "institution": null,
  "notes": null,
  "symbol": null,
  "costPerUnit": null,
  "currentPricePerUnit": null,
  "interestRate": null,
  "createdAt": "2026-11-22T11:56:51.683623Z",
  "updatedAt": "2026-11-22T11:56:53.310981Z"
}
```

#### E04 — PASS

`PUT /cash-flow-entries/CF6 không gửi linkedAssetId` → **200**

Impact AS4 sau PUT (confirmationRequired, recordTypes): `[false,["ASSET_VALUATION"]]`

```json
{
  "id": "c9c18de5-4337-46ef-9071-6b5822622b60",
  "entryType": "EXPENSE",
  "category": "INSURANCE",
  "amount": 3200000.00,
  "currency": "VND",
  "timing": "RECURRING",
  "recurrence": "YEARLY",
  "effectiveDate": "2026-03-01",
  "endDate": null,
  "linkedAssetId": null,
  "linkedLiabilityId": null,
  "notes": "Bảo hiểm xe Honda SH",
  "createdAt": "2026-11-22T11:56:52.140058Z",
  "updatedAt": "2026-11-22T11:56:53.420920Z"
}
```

## G — Guard xác nhận và xóa
#### G01 — PASS

`DELETE /assets/AS1` → **409**

`impact` so với GET deletion-impact ngay trước đó: giống. psql (asset | valuation | collateral LB1 | cash-flow link): `1|3|88d9ed07-7f99-43ca-af82-b008a2fce849|2`

```json
{
  "detail": "Deleting this record changes other records. Review the impact and repeat the request with confirm=true.",
  "instance": "/api/v1/assets/88d9ed07-7f99-43ca-af82-b008a2fce849",
  "status": 409,
  "title": "Linked records would change",
  "type": "urn:rikkaus:problem:linked-records",
  "impact": {
    "recordType": "ASSET",
    "recordId": "88d9ed07-7f99-43ca-af82-b008a2fce849",
    "confirmationRequired": true,
    "linkedEffects": [
      {
        "recordType": "ASSET_VALUATION",
        "effect": "DELETED",
        "field": null,
        "count": 3,
        "records": []
      },
      {
        "recordType": "LIABILITY",
        "effect": "UNLINKED",
        "field": "collateralAssetId",
        "count": 1,
        "records": [
          {
            "id": "95600651-67e4-4eed-843f-a66cfffa5058",
            "label": "Vay mua nhà Vietcombank (gói ưu đãi 2026)",
            "category": "BANK_LOAN"
          }
        ]
      },
      {
        "recordType": "CASH_FLOW_ENTRY",
        "effect": "UNLINKED",
        "field": "linkedAssetId",
        "count": 2,
        "records": [
          {
            "id": "f9564318-fc77-4890-a53c-12f89567ac74",
            "label": "Cho thuê căn hộ Thảo Điền",
            "category": "RENTAL_INCOME"
          },
          {
            "id": "0767fda4-43f4-4a6c-ba30-b98fbb647890",
            "label": "Phí quản lý căn hộ Thảo Điền",
            "category": "HOUSING"
          }
        ]
      }
    ],
    "derivedEffects": []
  },
  "correlationId": "3ba49854-aab8-40e4-98ef-4c20e46cca1f"
}
```

#### G02 — PASS

`DELETE /assets/AS1?confirm=false` → **409**

```json
{
  "detail": "Deleting this record changes other records. Review the impact and repeat the request with confirm=true.",
  "instance": "/api/v1/assets/88d9ed07-7f99-43ca-af82-b008a2fce849",
  "status": 409,
  "title": "Linked records would change",
  "type": "urn:rikkaus:problem:linked-records",
  "impact": {
    "recordType": "ASSET",
    "recordId": "88d9ed07-7f99-43ca-af82-b008a2fce849",
    "confirmationRequired": true,
    "linkedEffects": [
      {
        "recordType": "ASSET_VALUATION",
        "effect": "DELETED",
        "field": null,
        "count": 3,
        "records": []
      },
      {
        "recordType": "LIABILITY",
        "effect": "UNLINKED",
        "field": "collateralAssetId",
        "count": 1,
        "records": [
          {
            "id": "95600651-67e4-4eed-843f-a66cfffa5058",
            "label": "Vay mua nhà Vietcombank (gói ưu đãi 2026)",
            "category": "BANK_LOAN"
          }
        ]
      },
      {
        "recordType": "CASH_FLOW_ENTRY",
        "effect": "UNLINKED",
        "field": "linkedAssetId",
        "count": 2,
        "records": [
          {
            "id": "f9564318-fc77-4890-a53c-12f89567ac74",
            "label": "Cho thuê căn hộ Thảo Điền",
            "category": "RENTAL_INCOME"
          },
          {
            "id": "0767fda4-43f4-4a6c-ba30-b98fbb647890",
            "label": "Phí quản lý căn hộ Thảo Điền",
            "category": "HOUSING"
          }
        ]
      }
    ],
    "derivedEffects": []
  },
  "correlationId": "3c3f98f2-eb2a-4747-b241-bed9143b88d7"
}
```

#### G03 — PASS

`DELETE /liabilities/LB1` → **409**

psql: LB1 còn = 1

```json
{
  "detail": "Deleting this record changes other records. Review the impact and repeat the request with confirm=true.",
  "instance": "/api/v1/liabilities/95600651-67e4-4eed-843f-a66cfffa5058",
  "status": 409,
  "title": "Linked records would change",
  "type": "urn:rikkaus:problem:linked-records",
  "impact": {
    "recordType": "LIABILITY",
    "recordId": "95600651-67e4-4eed-843f-a66cfffa5058",
    "confirmationRequired": true,
    "linkedEffects": [
      {
        "recordType": "CASH_FLOW_ENTRY",
        "effect": "UNLINKED",
        "field": "linkedLiabilityId",
        "count": 1,
        "records": [
          {
            "id": "4c52ae22-9fc1-4a36-b3d5-7a3020a97c7f",
            "label": "Trả góp vay mua nhà Vietcombank",
            "category": "DEBT_PRINCIPAL_PAYMENT"
          }
        ]
      }
    ],
    "derivedEffects": []
  },
  "correlationId": "a073193e-3211-4a2a-b7a7-596ff994a9da"
}
```

#### G04 — PASS

`DELETE /assets/AS2` → **204**

psql (asset | valuation): `0|0`

```json
(empty)
```

#### G05 — PASS

`DELETE /liabilities/LB2` → **204**

```json
(empty)
```

#### G06 — PASS

`DELETE /cash-flow-entries/CF5` → **204**

GET /cash-flow-occurrences?from=2026-09-01&to=2026-09-30 → 200; số occurrence có sourceEntryId = CF5: trước khi xóa 1, sau khi xóa 0

```json
(empty)
```

#### G07 — PASS

`DELETE /goals/G1` → **204**

```json
(empty)
```

#### G08 — PASS

`DELETE /assets/AS4 (sau E04 không còn link)` → **204**

```json
(empty)
```

#### G09 — PASS

`DELETE /assets/AS1?confirm=true` → **204**

psql (asset | valuation | LB1 còn | collateral LB1 | CF1+CF2 còn | CF1+CF2 link NULL): `0|0|1|NULL|2|2`; GET /liabilities/LB1 → collateralAssetId = null

```json
(empty)
```

#### G10 — PASS

`DELETE /assets/AS1?confirm=true lần nữa` → **404**

```json
{
  "detail": "Resource not found.",
  "instance": "/api/v1/assets/88d9ed07-7f99-43ca-af82-b008a2fce849",
  "status": 404,
  "title": "Resource not found",
  "type": "urn:rikkaus:problem:not-found",
  "correlationId": "78ba22c1-693b-441d-9202-0e2f35dcce65"
}
```

#### G11 — PASS

`GET /liabilities/LB1/deletion-impact sau G09` → **200**

```json
{
  "recordType": "LIABILITY",
  "recordId": "95600651-67e4-4eed-843f-a66cfffa5058",
  "confirmationRequired": true,
  "linkedEffects": [
    {
      "recordType": "CASH_FLOW_ENTRY",
      "effect": "UNLINKED",
      "field": "linkedLiabilityId",
      "count": 1,
      "records": [
        {
          "id": "4c52ae22-9fc1-4a36-b3d5-7a3020a97c7f",
          "label": "Trả góp vay mua nhà Vietcombank",
          "category": "DEBT_PRINCIPAL_PAYMENT"
        }
      ]
    }
  ],
  "derivedEffects": []
}
```

#### G12 — PASS

`DELETE /liabilities/LB1?confirm=true` → **204**

psql (LB1 | CF3 còn | link CF3): `0|1|NULL`

```json
(empty)
```

#### G13 — PASS

`DELETE /cash-flow-entries/CF1?confirm=true` → **204**

```json
(empty)
```

## C — Đồng thời
#### C01 — PASS

`4 × DELETE /assets/AS3?confirm=true song song` → **204 404 404 404**

Problem types: ` 3 urn:rikkaus:problem:not-found;`; psql (AS3 | CF4 còn | link CF4): `0|1|NULL`

```json
(empty)
```

#### C02 — PASS

`4 × DELETE /assets/AS5 song song, không confirm` → **409 409 409 409**

Problem types: ` 4 urn:rikkaus:problem:linked-records;`; psql: AS5 còn = 1

```json
(empty)
```

#### C03 — PASS

`4 × DELETE /goals/G2 song song` → **204 404 404 404**

Problem types: ` 3 urn:rikkaus:problem:not-found;`

```json
(empty)
```

## K — Contract
#### K01 — PASS

`GET /v3/api-docs` → **200**

Kiểm tra (số path deletion-impact, confirm asset, 409 asset, confirm liability, 409 liability, enum linked-records, impact): `[4,true,true,true,true,true,true]`

```json
(empty)
```

#### K02 — PASS

`grep log ứng dụng` → **log**

Số dòng log chứa tên record hoặc `confirm=`: 0 / 155 dòng

```json
(empty)
```

## Tổng kết

PASS 45 / 45; FAIL 0 
