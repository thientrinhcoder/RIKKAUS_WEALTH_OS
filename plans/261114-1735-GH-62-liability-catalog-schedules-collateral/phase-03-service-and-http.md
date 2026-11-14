# Phase 3 — Service và HTTP

## Files

- `.../liabilities/application/LiabilityService.java`
- `.../liabilities/api/LiabilityController.java` — `@RequestMapping(ApiPaths.V1 + "/liabilities")`
- `.../liabilities/api/CreateLiabilityRequest.java`
- `.../liabilities/api/UpdateLiabilityRequest.java` — mọi trường của create trừ `category`
- `.../liabilities/api/LiabilityResponse.java`

## Service

| Method | Hành vi |
|---|---|
| `listFor(ownerId)` | repository list có thứ tự |
| `read(ownerId, id)` | `OwnershipGuard.requireOwned(findById(id), ownerId)` |
| `create(ownerId, category, details)` | normalize text → `rejectInconsistentInput` → `requireOwnedCollateral` → save |
| `replace(ownerId, id, details)` | requireOwned → normalize → reject → collateral → `replace` → save → `flushTreatingAVanishedRowAsNotFound` |
| `delete(ownerId, id)` | requireOwned → delete → flush như trên |

`rejectInconsistentInput` gom mọi lỗi vào một mảng `errors`:
- `maturityDate` trước `startDate` → `maturityDate: must not be before the start date`
- `nextPaymentDate` trước `startDate` → `nextPaymentDate: must not be before the start date`
- `nextPaymentDate` sau `maturityDate` → `nextPaymentDate: must not be after the maturity date` (chỉ khi
  maturity không bị từ chối vì trước start, để không báo lỗi thứ ba người dùng không có)
- amount có, frequency không → `recurringPaymentFrequency: must be present when a recurring payment amount is given`
- frequency có, amount không → `recurringPaymentAmount: must be present when a recurring payment frequency is given`

`requireOwnedCollateral`: nếu `collateralAssetId != null` và `!repository.ownsAsset(id, ownerId)` →
`validation-failed` `collateralAssetId: must identify an asset the signed-in user owns`. Chạy sau
`rejectInconsistentInput` và chỉ khi không có lỗi khác? Không: gom cùng mảng để một round trip báo đủ.

Text normalize: copy cách `AssetService.cleaned` (strip Unicode space separator, refuse U+0000, blank → null).
Lặp lại thay vì tách ra `shared` vì mới hai slice dùng; ghi follow-up khi slice thứ ba cần.

## Bean validation (request record)

| Field | Annotation |
|---|---|
| `name` | `@NotBlank`, `@Size(max=120)` |
| `category` (create) | `@NotNull` |
| `outstandingBalance` | `@NotNull`, `@DecimalMin("0")`, `@Digits(17,2)` |
| `currency` | `@NotNull` (`shared.money.Currency`) |
| `interestRate` | `@DecimalMin("0") "must not be negative"`, `@Digits(3,4) "must have at most 3 integer digits and 4 decimals"` |
| `recurringPaymentAmount` | `@DecimalMin(value="0", inclusive=false) "must be greater than zero"`, `@Digits(17,2)` |
| `notes` | `@Size(max=2000)` |

## Controller

Năm route, annotation OpenAPI đầy đủ status như `AssetController` (200/201/204, 400, 401, 404, 405, 406,
415), ví dụ Việt Nam thật trong `@ExampleObject`, header `Location` trên POST.

## Validation

Compile qua `ArchitectureRulesTest`; hành vi kiểm ở phase 5.
