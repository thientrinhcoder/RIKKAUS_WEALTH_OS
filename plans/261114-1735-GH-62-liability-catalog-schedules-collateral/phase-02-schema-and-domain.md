# Phase 2 — Schema và domain

## Context

Hình mẫu: `V4__assets.sql`, `Asset`, `AssetCategory`, `AssetRepository`. Mọi quy tắc tồn tại hai nơi:
bean validation ở biên API (field được đặt tên) và CHECK trong bảng (cho caller không phải API).

## Files

- `services/api/src/main/resources/db/migration/V7__liabilities.sql`
- `services/api/src/main/java/com/rikkaus/wealth/liabilities/domain/Liability.java`
- `.../domain/LiabilityCategory.java` — 6 giá trị
- `.../domain/PaymentFrequency.java` — `MONTHLY`, `QUARTERLY`, `SEMIANNUAL`, `YEARLY`
- `.../domain/LiabilityTerms.java` — record: interestRate, startDate, maturityDate
- `.../domain/PaymentSchedule.java` — record: recurringPaymentAmount, recurringPaymentFrequency, nextPaymentDate
- `.../domain/LiabilityDetails.java` — record mọi trường sửa được (name, balance, currency, terms, schedule, collateralAssetId, notes)
- `.../domain/LiabilityRepository.java` — `Repository<Liability, UUID>` hẹp: list có thứ tự, `findById`, `save`, `delete`, `flush`, `ownsAsset(assetId, userId)` native query

## Bảng `wealth.liabilities`

| Cột | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | PK |
| `user_id` | UUID | NOT NULL, FK `wealth.users` ON DELETE CASCADE |
| `name` | TEXT | NOT NULL, `btrim(name) <> ''`, ≤120 |
| `category` | TEXT | NOT NULL, IN 6 giá trị |
| `outstanding_balance` | NUMERIC(19,2) | NOT NULL, `>= 0` |
| `currency` | TEXT | NOT NULL, IN ('VND','USD') |
| `interest_rate` | NUMERIC(7,4) | NULL hoặc `>= 0` |
| `start_date` | DATE | |
| `maturity_date` | DATE | NULL hoặc start NULL hoặc `>= start_date` |
| `recurring_payment_amount` | NUMERIC(19,2) | NULL hoặc `> 0` |
| `recurring_payment_frequency` | TEXT | NULL hoặc IN 4 giá trị; cả hai cột lịch cùng NULL hoặc cùng NOT NULL |
| `next_payment_date` | DATE | trong `[start_date, maturity_date]` khi các mốc có |
| `collateral_asset_id` | UUID | FK `wealth.assets(id)` ON DELETE SET NULL |
| `notes` | TEXT | ≤2000 |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL |

Index: `idx_liabilities_owner_next_payment (user_id, next_payment_date)` phục vụ list.

## Steps

1. Viết migration với comment giải thích từng quyết định (ON DELETE SET NULL, NUMERIC(7,4), lịch đi đôi).
2. Entity `Liability implements UserOwned`, không setter, `create(...)` và `replace(details, now)`;
   `category` `updatable = false`; enum lưu `EnumType.STRING`.
3. Repository: `@Query("select l from Liability l where l.userId = :userId order by l.nextPaymentDate asc nulls last, l.name asc")`
   và `@Query(value = "select exists (select 1 from wealth.assets where id = :assetId and user_id = :userId)", nativeQuery = true) boolean ownsAsset(...)`.

## Validation

`verify` phải qua `ArchitectureRulesTest` (không import `assets`, không `double`) và
`ApplicationContextIT` sau khi thêm `"liabilities"` vào inventory bảng (phase 4). `ddl-auto: validate`
khớp entity với bảng khi API khởi động ở phase 5.

## Risk / rollback

Migration mới, bảng mới, không sửa bảng cũ: rollback là xóa file và bảng trên DB dùng một lần.
